package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ArgumentArray {
  /**
   * Flags whether or not a given method expects arguments as an array of Objects.
   * @return {@code true} if an array of Objects is expected. Otherwise, {@code false}.
   */
  public boolean value() default true;
}
