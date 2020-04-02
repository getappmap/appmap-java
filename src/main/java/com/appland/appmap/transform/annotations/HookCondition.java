package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.appland.appmap.process.conditions.Condition;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HookCondition {
  /**
   * Identifies a {@link Condition} class. When deciding whether to apply a hook to a behavior,
   * {@link Condition#match(javassist.CtBehavior)} must return true.
   * @return A class implementing {@link Condition}
   */
  public Class<? extends Condition> value() default Condition.class;
}
