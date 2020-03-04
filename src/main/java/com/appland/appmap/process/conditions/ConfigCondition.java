package com.appland.appmap.process.conditions;

import java.lang.reflect.Modifier;

import com.appland.appmap.config.AppMapConfig;

import javassist.CtBehavior;
import javassist.CtClass;

public abstract class ConfigCondition implements Condition {
  static {
    // HACK
    // force loading of AppMapConfig
    AppMapConfig.get();
  }

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