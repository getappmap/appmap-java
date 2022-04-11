package com.appland.appmap.transform;

import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.NoSourceAvailableException;
import com.appland.appmap.transform.annotations.Hook;
import com.appland.appmap.transform.annotations.HookSite;
import com.appland.appmap.transform.annotations.HookValidationException;
import com.appland.appmap.util.AppMapBehavior;
import com.appland.appmap.util.Logger;
import javassist.*;
import javassist.bytecode.Descriptor;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The ClassFileTransformer is responsible for loading and caching hooks during {@link com.appland.appmap.Agent}
 * statup. The {@link ClassFileTransformer#transform} method is used by the Instrumentation API to
 * modify class bytecode at load time. When a class is loaded, this class will attempt to apply
 * hooks to each behavior declared by that class.
 */
public class ClassFileTransformer implements java.lang.instrument.ClassFileTransformer {
  private static final List<Hook> unkeyedHooks = new ArrayList<>();
  private static final Map<String, List<Hook>> keyedHooks = new HashMap<>();

  /**
   * Default constructor. Caches hooks for future class transforms.
   */
  public ClassFileTransformer() {
    super();

    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setUrls(ClasspathHelper.forPackage("com.appland.appmap.process"))
        .setScanners(new SubTypesScanner(false))
        .filterInputsBy(new FilterBuilder().includePackage("com.appland.appmap.process")));
    ClassPool classPool = ClassPool.getDefault();
    for (Class<?> classType : reflections.getSubTypesOf(Object.class)) {
      try {
        CtClass ctClass = classPool.get(classType.getName());
        processClass(ctClass);
        ctClass.detach();
      } catch (NotFoundException e) {
        Logger.printf("failed to find %s in class pool", classType.getName());
        Logger.println(e);
      }
    } 
  }

  private void addHook(Hook hook) {
    if (hook == null) {
      return;
    }

    String key = hook.getKey();

    if (Properties.DebugHooks)
      Logger.printf("%s: %s\n", key, hook);

    if (key == null) {
      unkeyedHooks.add(hook);
    } else {
      List<Hook> matchingKeyedHooks = keyedHooks.computeIfAbsent(key, k -> new ArrayList<>());
      matchingKeyedHooks.add(hook);
    }
  }

  private List<Hook> getHooks(String methodId) {
    List<Hook> matchingKeyedHooks = keyedHooks.get(methodId);
    if (matchingKeyedHooks == null || matchingKeyedHooks.isEmpty()) {
      return unkeyedHooks;
    }

    return Stream.of(matchingKeyedHooks, unkeyedHooks)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparingInt(Hook::getPosition))
        .collect(Collectors.toList());
  }

  private void processClass(CtClass ctClass) {
    for (CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
      Hook hook = Hook.from(behavior);
      if (hook == null) {
        continue;
      }

      ctClass.defrost();

      try {
        hook.validate();
      } catch (HookValidationException e) {
        Logger.println("failed to validate hook");
        Logger.println(e);
        continue;
      }

      this.addHook(hook);

      if (Properties.DebugHooks)
        Logger.printf("registered hook %s\n", hook.toString());
    }
  }

  private void applyHooks(CtBehavior behavior) {
    try {
      final List<HookSite> hookSites = this.getHooks(behavior.getName())
          .stream()
          .map(hook -> hook.prepare(behavior))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      if (hookSites.size() < 1) {
        return;
      }

      Hook.apply(behavior, hookSites);

      if (Properties.DebugHooks) {
        for (HookSite hookSite : hookSites) {
          final Hook hook = hookSite.getHook();
          Logger.printf("hooked %s.%s%s on (%s) with %s\n",
                        behavior.getDeclaringClass().getName(),
                        behavior.getName(),
                        behavior.getMethodInfo().getDescriptor(),
                        hook.getMethodEvent().getEventString(),
                        hook);
        }
      }
    } catch (NoSourceAvailableException e) {
      Logger.println(e);
    }
  }

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class redefiningClass,
                          ProtectionDomain domain,
                          byte[] bytes) throws IllegalClassFormatException {
    ClassPool classPool = new ClassPool();
    classPool.appendClassPath(new LoaderClassPath(loader));

    try {
      CtClass ctClass;
      try {
        ctClass = classPool.makeClass(new ByteArrayInputStream(bytes));
      } catch (RuntimeException e) {
        // The class is frozen
        // We can defrost it and apply our changes, though in practice I've observed this to be
        // unstable. Particularly, exceptions thrown from the Groovy runtime due to missing methods.
        // There's likely a way to do this safely, but further investigation is needed.
        //
        // ctClass = classPool.get(className.replace('/', '.'));
        // ctClass.defrost();
        //
        // For now, just skip this class
        Logger.printf("Skipping class %s, failed making a new one: %s\n", className, e.getMessage());
        return bytes;
      }

      if (ctClass.isInterface()) {
        return bytes;
      }

      for (CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
        if (ignoreMethod(behavior)) {
          continue;
        }

        this.applyHooks(behavior);
      }

      return ctClass.toBytecode();
    } catch (Exception e) {
      // Don't allow this exception to propagate out of this method, because it will be swallowed
      // by sun.instrument.TransformerManager.
      Logger.println("An error occurred transforming class " + className);
      Logger.println(e.getClass() + ": " + e.getMessage());
      e.printStackTrace(System.err);
    }

    return bytes;
  }

  private boolean ignoreMethod(CtBehavior behavior) {
    if ( !(behavior instanceof CtMethod) ) {
      return false;
    }
    CtMethod method = (CtMethod) behavior;
    try {
      return behavior.getMethodInfo2().isConstructor() ||
          behavior.getMethodInfo2().isStaticInitializer() ||
          isGetter(method) ||
          isSetter(method) ||
          isIgnoredInstanceMethod(method);
    } catch (NotFoundException e) {
      Logger.println(e);
      return true;
    }
  }

  private boolean isIgnoredInstanceMethod(CtMethod method) {
    final int mods = method.getModifiers();
    if ( Modifier.isStatic(mods) || !new AppMapBehavior(method).isRecordable()) {
      return false;
    }

    final String methodName = method.getName();
    return 
        methodName.equals("equals") ||
        methodName.equals("hashCode") ||
        methodName.equals("iterator") ||
        methodName.equals("toString");
  }

  public static boolean isGetter(CtMethod method) throws NotFoundException {
    // KEG I'm getting exceptions like this when trying to use method.getReturnType():
    //
    // com.appland.shade.javassist.NotFoundException: java.lang.String
    //
    // The descriptor is used under the hood by javassist, and it provides
    // what we need, albeit in a cryptic format.
    String descriptor = method.getMethodInfo().getDescriptor();
    String methodName = method.getName();
    if (new AppMapBehavior(method).isRecordable() &&
        Descriptor.numOfParameters(descriptor) == 0) {
      if (methodName.matches("^get[A-Z].*") &&
          !descriptor.matches("\\)V$")) /* void */
        return true;
      if (methodName.matches("^is[A-Z].*") &&
          descriptor.matches("\\)Z$")) /* boolean */
        return true;
      /* boolean */
      return methodName.matches("^has[A-Z].*") &&
          descriptor.matches("\\)Z$");
    }
    return false;
  }

  public static boolean isSetter(CtMethod method) throws NotFoundException {
    String descriptor = method.getMethodInfo().getDescriptor();
    return new AppMapBehavior(method).isRecordable() &&
        descriptor.matches("\\)V$") /* void */ &&
        Descriptor.numOfParameters(descriptor) == 1 &&
        method.getName().matches("^set[A-Z].*");
  }
}
