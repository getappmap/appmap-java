package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HookAnnotated {
  /**
   * Identifies an annotation type of which a hook shall be applied to a method if it has this
   * annotation.
   * @return The fully qualified name of an annotation
   */
  public String value() default "";
}
