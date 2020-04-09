package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.appland.appmap.process.conditions.Condition;

/**
 * Code matching this {@link Condition} will be hooked.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HookCondition {
  /**
   * @return A class implementing {@link Condition}
   */
  public Class<? extends Condition> value() default Condition.class;
}
