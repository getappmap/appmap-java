package com.appland.appmap.transform;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.NoSourceAvailableException;
import com.appland.appmap.transform.annotations.AnnotationUtil;
import com.appland.appmap.transform.annotations.AnnotationUtil.AnnotatedBehavior;
import com.appland.appmap.transform.annotations.AnnotationUtil.AnnotatedClass;
import com.appland.appmap.transform.annotations.AppMapInstrumented;
import com.appland.appmap.transform.annotations.Hook;
import com.appland.appmap.transform.annotations.HookFactory;
import com.appland.appmap.transform.annotations.HookSite;
import com.appland.appmap.transform.annotations.HookValidationException;
import com.appland.appmap.util.AppMapClassPool;
import com.appland.appmap.util.Logger;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

/**
 * The ClassFileTransformer is responsible for loading and caching hooks during {@link com.appland.appmap.Agent}
 * statup. The {@link ClassFileTransformer#transform} method is used by the Instrumentation API to
 * modify class bytecode at load time. When a class is loaded, this class will attempt to apply
 * hooks to each behavior declared by that class.
 */
public class ClassFileTransformer implements java.lang.instrument.ClassFileTransformer {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static String tracePrefix = Properties.DebugClassPrefix;
  private static final String PROCESS_PACKAGE = "com.appland.appmap.process";

  private final String name;
  private final List<Hook> unkeyedHooks = new ArrayList<>();
  private final Map<String, List<Hook>> keyedHooks = new HashMap<>();
  private HookFactory hookFactory;

  private Hook[] sortedUnkeyedHooks = null;
  private Map<String, Hook[]> allKeyedHooks = null;

  // These stats are only intended to give a sense of how much work was done by
  // the transformers. It's not neccessary that they be absolutely correct, so
  // access to them isn't synchronized.
  private static final List<ClassFileTransformer> instances = new ArrayList<>();
  private long classesExamined = 0;
  private long methodsHooked = 0;
  private long methodsExamined = 0;
  private HashMap<String, Integer> packagesHooked = new HashMap<>();
  private HashMap<String, Integer> packagesIgnored = new HashMap<>();
  private long classesIgnored = 0;

  /**
   * Default constructor. Caches hooks for future class transforms.
   */
  public ClassFileTransformer(String name, HookFactory hookFactory) {
    super();
    this.name = name;
    this.hookFactory = hookFactory;

    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setUrls(ClasspathHelper.forPackage(PROCESS_PACKAGE))
        .setScanners(new SubTypesScanner(false))
        .filterInputsBy(new FilterBuilder().includePackage(PROCESS_PACKAGE)));
    ClassPool classPool = AppMapClassPool.acquire(Thread.currentThread().getContextClassLoader());
    try {
      for (Class<?> classType : reflections.getSubTypesOf(Object.class)) {
        try {
          CtClass ctClass = classPool.get(classType.getName());
          processClass(ctClass);
          ctClass.detach();
        } catch (NotFoundException e) {
          logger.debug(e);
        }
      }
      resolveHooks();
    } finally {
      AppMapClassPool.release();
      instances.add(this);
    }
  }

  private void resolveHooks() {
    // @formatter:off
    Function<Stream<Hook>, Hook[]> sorter = s -> s.sorted(
        Comparator.comparingInt(Hook::getPosition)
      )
      .toArray(Hook[]::new);

    sortedUnkeyedHooks = sorter.apply(unkeyedHooks.stream());

    allKeyedHooks = keyedHooks
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> sorter.apply(
          Stream.of(
            e.getValue(),
            unkeyedHooks
          )
          .flatMap(Collection::stream)))
      );
    // @formatter:on
  }

  private void addHook(Hook hook) {
    if (hook == null) {
      return;
    }

    String key = hook.getKey();

    logger.trace("{}: {}", key, hook);

    if (key == null) {
      unkeyedHooks.add(hook);
    } else {
      List<Hook> matchingKeyedHooks = keyedHooks.computeIfAbsent(key, k -> new ArrayList<>());
      matchingKeyedHooks.add(hook);
    }
  }

  private Hook[] getHooks(String methodId) {
    Hook[] methodHooks = allKeyedHooks.get(methodId);
    return methodHooks != null ? methodHooks : sortedUnkeyedHooks;
  }

  private void processClass(CtClass ctClass) {
    boolean traceClass = tracePrefix == null || ctClass.getName().startsWith(tracePrefix);

    if (traceClass) {
      logger.trace(() -> ctClass.getName());
    }

    for (CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
      if (traceClass) {
        logger.trace(() -> behavior.getLongName());
      }
      Hook hook = hookFactory.from(behavior);
      if (hook == null) {
        if (traceClass) {
          logger.trace("{}, no hooks", () -> behavior.getLongName());
        }
        continue;
      }

      ctClass.defrost();

      try {
        hook.validate();
      } catch (HookValidationException e) {
        logger.debug(e, "failed to validate hook");
        continue;
      }

      this.addHook(hook);

      if (traceClass) {
        logger.trace("registered hook {}", hook);
      }
    }
  }

  private boolean applyHooks(CtBehavior behavior) {
    boolean traceClass = tracePrefix == null || behavior.getDeclaringClass().getName().startsWith(tracePrefix);

    try {
      List<HookSite> hookSites = getHookSites(behavior);

      if (hookSites == null) {
        if (traceClass) {
          logger.trace("no hook sites");
        }
        return false;
      }

      Hook.apply(behavior, hookSites);

      if (logger.isDebugEnabled()) {
        for (HookSite hookSite : hookSites) {
          final Hook hook = hookSite.getHook();
          String className = behavior.getDeclaringClass().getName();
          if (tracePrefix != null && !className.startsWith(tracePrefix)) {
            continue;
          }

          if (traceClass) {
            logger.trace("hooked {}.{}{} on ({},{}) with {}",
                className,
                behavior.getName(),
                behavior.getMethodInfo().getDescriptor(),
                hook.getMethodEvent().getEventString(),
                hook.getPosition(),
                hook);
          }
        }
      }
      return true;

    } catch (NoSourceAvailableException e) {
      Logger.println(e);
    }

    return false;
  }

  public List<HookSite> getHookSites(CtBehavior behavior) {
    List<HookSite> hookSites = null;
    Map<String, Object> hookContext = new HashMap<>();

    // Put this behavior's annotations in the context so they'll be available
    // when the hooks are prepared.
    AnnotatedBehavior ab = new AnnotatedBehavior(behavior);
    AnnotationsAttribute attr = ab.get();
    Set<String> behaviorAnnotations = null;
    if (attr != null) {
      behaviorAnnotations = new HashSet<>();
      Annotation[] annotations = attr.getAnnotations();
      for (Annotation a : annotations) {
        behaviorAnnotations.add(a.getTypeName());
      }
    }
    hookContext.put(Hook.ANNOTATIONS, behaviorAnnotations);

    for (Hook hook : getHooks(behavior.getName())) {
      HookSite hookSite = hook.prepare(behavior, hookContext);
      if (hookSite == null) {
        continue;
      }

      if (hookSites == null) {
        hookSites = new ArrayList<>();
      }
      hookSites.add(hookSite);
    }
    return hookSites;
  }

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class<?> redefiningClass,
                          ProtectionDomain domain,
      byte[] bytes) throws IllegalClassFormatException {

    classesExamined++;
    AppMapClassPool.acquire(loader);
    try {
      // Anonymous classes created by sun.misc.Unsafe.defineAnonymousClass don't
      // have names.
      if (className == null) {
        return null;
      }

      className = className.replace('/', '.');
      if (className.startsWith("com.appland")
          && !className.startsWith("com.appland.appmap.test.fixture")) {
        return null;
      }

      boolean traceClass = tracePrefix == null || className.startsWith(tracePrefix);

      CtClass ctClass;
      try {
        ClassPool classPool = AppMapClassPool.get();
        if (traceClass) {
          logger.debug("className: {}", className);
        }

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
        logger.warn(e, "makeClass failed");
        return null;
      }

      if (ctClass.isInterface()) {
        if (traceClass) {
          logger.trace("{} is an interface", className);
        }
        return null;
      }

      boolean hookApplied = false;
      for (CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
        if (traceClass) {
          logger.trace("behavior: {}", behavior.getLongName());
        }

        if ((behavior.getModifiers() & Modifier.ABSTRACT) != 0) {
          if (traceClass) {
            logger.trace("abstract method");
          }
          continue;
        }

        methodsExamined++;
        if (this.applyHooks(behavior)) {
          hookApplied = true;
          methodsHooked++;
        }
      }

      if (hookApplied) {
        // One or more of the methods in the the class were marked as needing to
        // be instrumented. Mark the class so the bytebuddy transformer will
        // know it needs to be instrumented.
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();
        Annotation annot = new Annotation(AppMapInstrumented.class.getName(), constPool);
        AnnotationUtil.setAnnotation(new AnnotatedClass(ctClass), annot);

        if (traceClass) {
          logger.trace("hooks applied to {}", className);
        }
        if (logger.isDebugEnabled()) {
          packagesHooked.compute(ctClass.getPackageName(), (k, v) -> v == null ? 1 : v + 1);
        }

        return ctClass.toBytecode();
      }

      classesIgnored++;
      if (logger.isDebugEnabled()) {
        packagesIgnored.compute(ctClass.getPackageName(), (k, v) -> v == null ? 1 : v + 1);
      }

      if (traceClass) {
        logger.trace("no hooks applied to {}, methods: {}", ctClass::getName,
            () -> Arrays.stream(ctClass.getDeclaredBehaviors())
                .map(CtBehavior::getName).collect(Collectors.joining(",")));
      }

    } catch (Throwable t) {
      // Don't allow this exception to propagate out of this method, because it will
      // be swallowed
      // by sun.instrument.TransformerManager.
      logger.warn(t);
    } finally {
      AppMapClassPool.release();
    }

    return null;
  }

  public static void logStatistics() {
    instances.forEach(cft -> {
      logger.info("+++ {} +++", cft.name);

      logger.info("classes examined: {}", cft.classesExamined);
      logger.info("classes ignored: {}", cft.classesIgnored);
      logger.info("methods examined: {}", cft.methodsExamined);
      logger.info("methods instrumented: {}", cft.methodsHooked);

      logger.debug("{} packages hooked:\n{}", () -> cft.packagesHooked.size(),
          () -> collectPkgs(cft.packagesHooked));
      logger.debug("{} packages ignored:\n{}", () -> cft.packagesIgnored.size(),
          () -> collectPkgs(cft.packagesIgnored));

      logger.info("=== {} ===", cft.name);
    });
  }

  @SuppressWarnings("unchecked")
  private static String collectPkgs(HashMap<String, Integer> p) {
    HashMap<String, Integer> pkgs = (HashMap<String, Integer>)p.clone();
    return pkgs.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .map(entry -> entry.getKey() + ": " + entry.getValue())
        .collect(Collectors.joining("\n"));
  }
}
