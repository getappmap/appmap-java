package com.appland.appmap.process.conditions;

import java.util.Map;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;
import com.appland.appmap.util.AppMapBehavior;
import com.appland.appmap.util.FullyQualifiedName;
import com.appland.appmap.util.Logger;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;


/**
 * ConfigCondition checks if the behavior should be hooked due to its inclusion in the
 * {@link AppMapConfig}.
 * @see AppMapConfig
 */
public abstract class ConfigCondition implements Condition {
  static {
    // HACK
    // force loading of AppMapConfig to prevent it from being loaded twice
    AppMapConfig.get();
  }

  /**
   * Determines whether the given behavior should be hooked due to its inclusion in the global
   * {@link AppMapConfig}.
   * @param behavior A behavior being loaded
   * @return {@code true} if the behavior should be hooked
   * @see AppMapConfig
   */
  public static Boolean match(CtBehavior behavior, Map<String, Object> matchResult) {
    CtClass declaringClass = behavior.getDeclaringClass();
    if (declaringClass.getName().startsWith("java.lang")) {
      return false;
    }

    if (!new AppMapBehavior(behavior).isRecordable() || ignoreMethod(behavior)) {
      return false;
    }

    if (!declaringClass.isInterface() && behavior.getMethodInfo().getLineNumber(0) < 0) {
      // likely a runtime generated method
      return false;
    }

    final AppMapPackage.LabelConfig ls = AppMapConfig.get().includes(new FullyQualifiedName(behavior));
    if (ls != null) {
      matchResult.put("labels", ls.getLabels());
      return true;
    }

    return false;
  }

  static boolean isSetter(CtMethod method) throws NotFoundException {
    String descriptor = method.getMethodInfo().getDescriptor();
    return new AppMapBehavior(method).isRecordable() && descriptor.matches(".*\\)V$") /* void */
        && Descriptor.numOfParameters(descriptor) == 1 && method.getName().matches("^set[A-Z].*");
  }

  static boolean isGetter(CtMethod method) throws NotFoundException {
    // KEG I'm getting exceptions like this when trying to use method.getReturnType():
    //
    // com.appland.shade.javassist.NotFoundException: java.lang.String
    //
    // The descriptor is used under the hood by javassist, and it provides
    // what we need, albeit in a cryptic format.
    String descriptor = method.getMethodInfo().getDescriptor();
    String methodName = method.getName();
    if (new AppMapBehavior(method).isRecordable() && Descriptor.numOfParameters(descriptor) == 0) {
      if (methodName.matches("^get[A-Z].*") && !descriptor.matches(".*\\)V$")) {/* void */
        return true;
      }

      if (methodName.matches("^is[A-Z].*") && descriptor.matches(".*\\)Z$")) {/* boolean */
        return true;
      }
      /* boolean */
      return methodName.matches("^has[A-Z].*") && descriptor.matches(".*\\)Z$");
    }
    return false;
  }

  private static boolean ignoreMethod(CtBehavior behavior) {
    if (!(behavior instanceof CtMethod)) {
      return false;
    }

    CtMethod method = (CtMethod)behavior;
    try {
      return behavior.getMethodInfo2().isConstructor()
          || behavior.getMethodInfo2().isStaticInitializer()
          || ConfigCondition.isGetter(method)
          || ConfigCondition.isSetter(method)
          || isIgnoredInstanceMethod(method);
    } catch (NotFoundException e) {
      Logger.println(e);
      return true;
    }
  }

  public static boolean isIgnoredInstanceMethod(CtMethod method) {
    final int mods = method.getModifiers();
    if (Modifier.isStatic(mods)) {
      return false;
    }

    final String methodName = method.getName();
    return methodName.equals("equals") ||
        methodName.equals("hashCode") ||
        methodName.equals("iterator") ||
        methodName.equals("toString");
  }
}
