package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a hook does not have a receiver parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ExcludeReceiver {
  /**
   * @return {@code true} if a hooked method should ignore the receiver. Otherwise, {@code false}.
   */
  public boolean value() default true;
}
