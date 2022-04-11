package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes matching this annotation will be hooked.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HookClass {
  /**
   * Identifies the name of a class of which a hook shall be applied if the method name and its
   * parameters are applicable.
   * @return The fully qualified name of a class
   */
  public String value() default "";

  /**
   * If set, overrides the method name to apply a hook to. By default, the name of the method this
   * annotation is attached to will be used.
   * @return A method name to apply this hook to.
   */
  public String method() default "";

  public int position() default ISystem.HOOK_POSITION_DEFAULT;
}
