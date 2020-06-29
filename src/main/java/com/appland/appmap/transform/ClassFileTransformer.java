package com.appland.appmap.transform;

import com.appland.appmap.output.v1.NoSourceAvailableException;
import com.appland.appmap.transform.annotations.Hook;
import com.appland.appmap.transform.annotations.HookSite;
import com.appland.appmap.transform.annotations.HookValidationException;
import com.appland.appmap.util.Logger;
import javassist.*;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The ClassFileTransformer is responsible for loading and caching hooks during {@link Agent}
 * statup. The {@link ClassFileTransformer#transform} method is used by the Instrumentation API to
 * modify class bytecode at load time. When a class is loaded, this class will attempt to apply
 * hooks to each behavior declared by that class.
 */
public class ClassFileTransformer implements java.lang.instrument.ClassFileTransformer {
  private static final List<Hook> unkeyedHooks = new ArrayList<Hook>();
  private static final HashMap<String, List<Hook>> keyedHooks = new HashMap<String, List<Hook>>();

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
    for (Class<? extends Object> classType : reflections.getSubTypesOf(Object.class)) {
      try {
        CtClass ctClass = classPool.get(classType.getName());
        processClass(ctClass);
      } catch (NotFoundException e) {
        Logger.printf("AppMap: failed to find %s in class pool", classType.getName());
        Logger.println(e.getMessage());
      }
    } 
  }

  private void addHook(Hook hook) {
    if (hook == null) {
      return;
    }

    String key = hook.getKey();

    Logger.printf("%s: %s\n", key, hook);

    if (key == null) {
      unkeyedHooks.add(hook);
    } else {
      List<Hook> matchingKeyedHooks = keyedHooks.get(key);
      if (matchingKeyedHooks == null) {
        matchingKeyedHooks = new ArrayList<Hook>();
        keyedHooks.put(key, matchingKeyedHooks);
      }
      matchingKeyedHooks.add(hook);
    }
  }

  private List<Hook> getHooks(String methodId) {
    List<Hook> matchingKeyedHooks = keyedHooks.get(methodId);
    if (matchingKeyedHooks == null || matchingKeyedHooks.isEmpty()) {
      return unkeyedHooks;
    }

    return Stream.of(matchingKeyedHooks, unkeyedHooks)
        .flatMap(x -> x.stream())
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
        Logger.println("AppMap: failed to validate hook");
        Logger.println(e.getMessage());
        continue;
      }

      this.addHook(hook);

      Logger.printf("AppMap: registered hook %s\n", hook.toString());
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

      for (HookSite hookSite : hookSites) {
        final Hook hook = hookSite.getHook();
        Logger.printf("AppMap: hooked %s.%s (%s) with %s\n",
              behavior.getDeclaringClass().getName(),
              behavior.getName(),
              hook.getMethodEvent().getEventString(),
              hook);
      }
    } catch (NoSourceAvailableException e) {
      return;
    }
  }

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class redefiningClass,
                          ProtectionDomain domain,
                          byte[] bytes) throws IllegalClassFormatException {
    ClassPool classPool = new ClassPool(true);
    classPool.appendClassPath(new LoaderClassPath(loader));

    try {
      CtClass ctClass = null;

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
        return bytes;
      }

      if (ctClass.isInterface()) {
        return bytes;
      }

      for (CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
        if (behavior instanceof CtConstructor) {
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
}
