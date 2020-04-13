package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Unique {
  /**
   * Applies a unique key restraint on this hook. A call stack can, at most, contain a single
   * {@link Event} for each unique key. Without a unique key, no restrictions are applied.
   * @return The unique key
   */
  public String value() default "";
}
