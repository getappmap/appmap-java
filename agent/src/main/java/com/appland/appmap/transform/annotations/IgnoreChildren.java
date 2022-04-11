package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface IgnoreChildren {
  /**
   * Indicates whether or not {@link HookClass} should match children.
   * @return {@code true} if children should be ignored. Otherwise, {@code false}.
   */
  public boolean value() default true;
}
