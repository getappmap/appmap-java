package com.appland.appmap.transform.annotations;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.process.conditions.Condition;

import javassist.CtBehavior;

public class HookConditionSystem extends SourceMethodSystem {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private static final Map<Class<? extends Condition>, Condition> conditions = new HashMap<>();
  private Condition condition;

  private HookConditionSystem(CtBehavior behavior, Condition condition) {
    super(behavior, HookCondition.class);

    this.condition = Objects.requireNonNull(condition);
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

      return new HookConditionSystem(behavior, conditions.computeIfAbsent(conditionClass, c -> {
        try {
          return c.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
          logger.warn(e);
        }
        return null;
      }));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Boolean match(CtBehavior behavior, Map<String, Object> mapResult) {
    return this.condition.match(behavior, mapResult);
  }

  @Override
  public Integer getHookPosition() {
    return ISystem.HOOK_POSITION_DEFAULT;
  }
}
