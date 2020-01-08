package com.appland.appmap.transform.metadata;

import com.appland.appmap.config.AppMapConfig;
import java.lang.annotation.Annotation;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;


public class HookableConfigPath extends Hookable {
  @Override
  protected Boolean match(CtBehavior behavior) {
    if (!Modifier.isPublic(behavior.getModifiers())) {
      return false;
    }

    if (behavior.getMethodInfo().getLineNumber(0) < 0) {
      // likely a runtime generated method
      return false;
    }

    final String className = behavior.getDeclaringClass().getName();
    if (className.contains("$")) {
      return false;
    }

    return AppMapConfig.get().includes(className);
  }
}