package com.appland.appmap.process.conditions;

import java.util.Map;
import java.util.regex.Pattern;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;
import com.appland.appmap.config.Properties;
import com.appland.appmap.transform.annotations.AnnotationUtil;
import com.appland.appmap.transform.annotations.AnnotationUtil.AnnotatedBehavior;
import com.appland.appmap.transform.annotations.AppMapAppMethod;
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
public class ConfigCondition implements Condition {
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
  public Boolean match(CtBehavior behavior, Map<String, Object> matchResult) {
    boolean matched = doMatch(behavior, matchResult);
    if (matched) {
      AnnotationUtil.setAnnotation(new AnnotatedBehavior(behavior), AppMapAppMethod.class);
    }
    return matched;
  }

  private boolean doMatch(CtBehavior behavior, Map<String, Object> matchResult) {
    CtClass declaringClass = behavior.getDeclaringClass();
    String declaringClassName = declaringClass.getName();
    for (String p : Properties.IgnoredPackages) {
      if (declaringClassName.startsWith(p)) {
        return false;
      }
    }

    if (!AppMapBehavior.isRecordable(behavior) || ignoreMethod(behavior)) {
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

  private static final Pattern SETTER_PATTERN = Pattern.compile("^set[A-Z].*");

  static boolean isSetter(CtMethod method) throws NotFoundException {
    String descriptor = method.getMethodInfo().getDescriptor();

    return AppMapBehavior.isRecordable(method) &&
        Descriptor.numOfParameters(descriptor) == 1 &&
        descriptor.endsWith(")V") &&
        SETTER_PATTERN.matcher(method.getName()).matches();
  }

  private static final Pattern GETTER_PATTERN = Pattern.compile("^get[A-Z].*");
  private static final Pattern IS_HAS_PATTERN = Pattern.compile("^(i|ha)s[A-Z].*");

  static boolean isGetter(CtMethod method) throws NotFoundException {
    // KEG I'm getting exceptions like this when trying to use method.getReturnType():
    //
    // com.appland.shade.javassist.NotFoundException: java.lang.String
    //
    // The descriptor is used under the hood by javassist, and it provides
    // what we need, albeit in a cryptic format.
    String descriptor = method.getMethodInfo().getDescriptor();
    String methodName = method.getName();
    if (AppMapBehavior.isRecordable(method) && Descriptor.numOfParameters(descriptor) == 0) {
      if (!descriptor.endsWith(")V") && GETTER_PATTERN.matcher(methodName).matches()) {
        return true;
      }

      return descriptor.endsWith(")Z") && IS_HAS_PATTERN.matcher(methodName).matches();
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
