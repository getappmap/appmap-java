package com.appland.appmap.transform;

import com.appland.appmap.config.AppMapConfig;
import java.lang.annotation.Annotation;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;


/**
 * HookableConfigPath matches a class if it's listed in the <code>appmap.yaml</code>.
 */
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
    return AppMapConfig.get().includes(className);
  }
}
