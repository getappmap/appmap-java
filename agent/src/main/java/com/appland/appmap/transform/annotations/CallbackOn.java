package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a hook to receive the method return value or method exception.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CallbackOn {
  /**
   * Indicates the point in which a hook is to be injected into a method.
   * @return {@code METHOD_INVOCATION} if the hook should be installed before the method body.
   *         {@code METHOD_RETURN} if the hook should be installed just before the method returns.
   *         {@code METHOD_EXCEPTION} if the hook should be installed in a {@code catch} block.
   */
  public MethodEvent value() default MethodEvent.METHOD_INVOCATION;
}
