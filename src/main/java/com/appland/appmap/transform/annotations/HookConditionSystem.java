package com.appland.appmap.transform.annotations;

import com.appland.appmap.process.conditions.Condition;
import com.appland.appmap.util.Logger;

import javassist.CtBehavior;

import java.lang.reflect.Method;

public class HookConditionSystem extends SourceMethodSystem {
  private Method conditionMethod = null;

  private HookConditionSystem(CtBehavior behavior, Method conditionMethod) {
    super(behavior);

    this.conditionMethod = conditionMethod;
  }

  /**
   * Factory method. Reads any relevant annotation information and caches it.
   * @param behavior The hook behavior
   * @return A new {@code HookConditionSystem} if {@link HookCondition} is found. Otherwise,
   *         {@code null}.
   */
  public static ISystem from(CtBehavior behavior) {
    try {
      HookCondition hookCondition = (HookCondition) behavior.getAnnotation(HookCondition.class);
      if (hookCondition == null) {
        hookCondition = (HookCondition) behavior
            .getDeclaringClass()
            .getAnnotation(HookCondition.class);
      }

      if (hookCondition == null) {
        return null;
      }

      Class<? extends Condition> conditionClass = hookCondition.value();
      if (conditionClass == null) {
        return null;
      }

      Method conditionMethod = conditionClass.getMethod("match", CtBehavior.class);
      if (conditionMethod == null) {
        return null;
      }

      return new HookConditionSystem(behavior, conditionMethod);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Boolean match(CtBehavior behavior) {
    try {
      return (Boolean) this.conditionMethod.invoke(null, behavior);
    } catch (Exception e) {
      Logger.printf("match failed due to %s exception\n", e.getClass().getName());
      Logger.println(e);
      return false;
    }
  }
}
