package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Code which has this annotation will be hooked.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HookAnnotated {
  /**
   * @return The fully qualified name of an annotation
   */
  public String value() default "";

  public MethodEvent methodEvent() default MethodEvent.METHOD_INVOCATION;
}
