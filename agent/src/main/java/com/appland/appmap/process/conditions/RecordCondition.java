package com.appland.appmap.process.conditions;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.util.AppMapBehavior;
import com.appland.appmap.util.FullyQualifiedName;
import com.appland.appmap.util.StringUtil;
import javassist.CtBehavior;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

/**
 * RecordCondition checks if the behavior should be recorded due to its inclusion in the
 * {@link Properties#Records}.
 * @see Properties#Records
 */
public abstract class RecordCondition implements Condition {

  /**
   * Determines whether the given behavior should be recorded due to its inclusion in the
   * {@link Properties#Records}.
   * @param behavior A behavior being loaded
   * @return {@code true} if the behavior should be recorded
   * @see Properties#Records
   */
  public static Boolean match(CtBehavior behavior, Map<String, Object> matchResult) {
    if (behavior.getDeclaringClass().getName().startsWith("java.lang")) {
      return false;
    }

    if (!new AppMapBehavior(behavior).isRecordable()) {
      return false;
    }

    if (behavior.getMethodInfo().getLineNumber(0) < 0) {
      // likely a runtime generated method
      return false;
    }
    final FullyQualifiedName canonicalName = new FullyQualifiedName(behavior);
    return Arrays.stream(Properties.getRecords()).anyMatch(
            record ->  record.equals(canonicalName.toString())) &&
          AppMapConfig.get().includes(canonicalName);
  }
}
