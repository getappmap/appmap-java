package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ContinueHooking {
  /**
   * Instructs a hook to ignore the global lock.
   * @return {@code true} if the global lock should be ignored. Otherwise, {@code false}.
   */
  public boolean value() default true;
}
