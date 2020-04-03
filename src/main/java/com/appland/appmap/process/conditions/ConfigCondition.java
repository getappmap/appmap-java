package com.appland.appmap.process.conditions;

import com.appland.appmap.config.AppMapConfig;

import java.lang.reflect.Modifier;

import javassist.CtBehavior;
import javassist.CtClass;

/**
 * ConfigCondition checks if the behavior should be hooked due to its inclusion in the
 * {@link AppMapConfig}.
 * @see AppMapConfig
 */
public abstract class ConfigCondition implements Condition {
  static {
    // HACK
    // force loading of AppMapConfig
    AppMapConfig.get();
  }

  /**
   * Determines whether the given behavior should be hooked due to its inclusion in the global
   * {@link AppMapConfig}.
   * @param behavior A behavior being loaded
   * @return {@code true} if the behavior should be hooked
   * @see AppMapConfig
   */
  public static Boolean match(CtBehavior behavior) {
    if (behavior.getDeclaringClass().getName().startsWith("java.lang")) {
      return false;
    }

    if (!Modifier.isPublic(behavior.getModifiers())) {
      return false;
    }

    if (behavior.getMethodInfo().getLineNumber(0) < 0) {
      // likely a runtime generated method
      return false;
    }

    return AppMapConfig.get().includes(behavior.getDeclaringClass().getName(),
            behavior.getMethodInfo().getName(),
            Modifier.isStatic(behavior.getModifiers()));
  }
}
