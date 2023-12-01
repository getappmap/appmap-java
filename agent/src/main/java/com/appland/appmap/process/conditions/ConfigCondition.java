package com.appland.appmap.process.conditions;

import java.util.Map;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;
import com.appland.appmap.util.AppMapBehavior;
import com.appland.appmap.util.FullyQualifiedName;
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

    if (!new AppMapBehavior(behavior).isRecordable()) {
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
}
