package com.appland.appmap.transform.annotations;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import javassist.CtBehavior;

import java.lang.reflect.Modifier;

import static com.appland.appmap.util.StringUtil.canonicalName;

public class HookRecordsSystem extends SourceMethodSystem {

  private String[] records;

  private HookRecordsSystem(CtBehavior behavior, String[] records) {
    super(behavior);
    this.records = records;
  }

  /**
   * Factory method. Reads any relevant annotation information and caches it.
   * @param behavior The hook behavior
   * @return A new {@code HookRecordsSystem} if {@link HookRecords} is found. Otherwise,
   *         {@code null}.
   */
  public static ISystem from(CtBehavior behavior) {
    Object hookRecordsAnnotation = null;
    try{
      hookRecordsAnnotation = behavior.getAnnotation(HookRecords.class);
      if (hookRecordsAnnotation == null) {
        hookRecordsAnnotation = behavior.getDeclaringClass().getAnnotation(HookRecords.class);
      }
    } catch (Exception ex){
      // do nothing
    }
    if (hookRecordsAnnotation == null) {
      return null;
    }
    return new HookRecordsSystem(behavior, Properties.Records);
  }

  @Override
  public Boolean match(CtBehavior behavior) {
    final Boolean isExplicitlyExcluded = AppMapConfig.get().excludes(
        behavior.getDeclaringClass().getName(),
        behavior.getMethodInfo().getName(),
        Modifier.isStatic(behavior.getModifiers()));

    return isMatches(behavior) && !isExplicitlyExcluded;
  }

  private Boolean isMatches(CtBehavior behavior){
    for (String record : this.records){
      if (canonicalName(behavior.getDeclaringClass().getName(),
              behavior.getMethodInfo().getName(),
              Modifier.isStatic(behavior.getModifiers())).startsWith(record)) {
        return true;
      }
    }
    return false;
  }
}
