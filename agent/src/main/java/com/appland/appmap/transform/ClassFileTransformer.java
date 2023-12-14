package com.appland.appmap.transform;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.appland.appmap.transform.annotations.Hook;
import com.appland.appmap.transform.annotations.HookSite;
import com.appland.appmap.transform.annotations.HookValidationException;
import com.appland.appmap.util.AppMapClassPool;
import com.appland.appmap.util.Logger;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;

/**
 * The ClassFileTransformer is responsible for loading and caching hooks during {@link com.appland.appmap.Agent}
 * statup. The {@link ClassFileTransformer#transform} method is used by the Instrumentation API to
 * modify class bytecode at load time. When a class is loaded, this class will attempt to apply
 * hooks to each behavior declared by that class.
 */
public class ClassFileTransformer implements java.lang.instrument.ClassFileTransformer {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static String tracePrefix = Properties.DebugClassPrefix;
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
    } finally {
      AppMapClassPool.release();
    }
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

  public static List<Hook> getHooks(String methodId) {
    List<Hook> matchingKeyedHooks = keyedHooks.get(methodId);
    if (matchingKeyedHooks == null) {
      matchingKeyedHooks = new ArrayList<Hook>();
    }

    return Stream.of(matchingKeyedHooks, unkeyedHooks)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparingInt(Hook::getPosition))
        .collect(Collectors.toList());
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
      Hook hook = Hook.from(behavior);
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
      final List<HookSite> hookSites = getHooks(behavior.getName())
          .stream()
          .map(hook -> hook.prepare(behavior))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      if (hookSites.size() < 1) {
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
            logger.debug("hooked {}.{}{} on ({},{}) with {}",
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

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class<?> redefiningClass,
                          ProtectionDomain domain,
      byte[] bytes) throws IllegalClassFormatException {

    AppMapClassPool.acquire(loader);
    try {
      // Anonymous classes created by sun.misc.Unsafe.defineAnonymousClass don't
      // have names.
      if (className == null) {
        return null;
      }

      className = className.replaceAll("/", ".");
      boolean traceClass = tracePrefix == null || className.startsWith(tracePrefix);

      CtClass ctClass;
      try {
        ClassPool classPool = AppMapClassPool.get();
        if (traceClass) {
          logger.trace("className: {}, classPool: {}", className, classPool);
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
          logger.trace("behavior: {} ", behavior.getLongName());
        }

        if ((behavior.getModifiers() & Modifier.ABSTRACT) != 0) {
          if (traceClass) {
            logger.trace("abstract method");
          }
          continue;
        }

        if (this.applyHooks(behavior)) {
          hookApplied = true;
        }
      }

      if (hookApplied) {
        if (traceClass) {
          logger.trace("hooks applied to {}", className);
        }

        return ctClass.toBytecode();
      }

      if (traceClass) {
        logger.trace("no hooks applied to {}", className);
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
}
