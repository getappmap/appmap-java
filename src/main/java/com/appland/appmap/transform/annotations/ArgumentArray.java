package com.appland.appmap.transform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instructs the hooking mechanism to pass parameters to the hook function as an Object[].
 * For example, suppose there is an instance method <code>public String getGreeting(String name)</code>
 * on <code>MyClass</code>.
 * A hook for this method that does not use ArgumentArray should have the signature
 * <code>public static void myHook(Event e, MyClass receiver, String name)</code>. With the ArgumentArray
 * annotation on <code>myHook</code>, the method signature should be
 * <code>public static void myHook(Event e, MyClass receiver, Object[] args)</code>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ArgumentArray {
  /**
   * Flags whether or not a given method expects arguments as an array of Objects.
   * @return {@code true} if an array of Objects is expected. Otherwise, {@code false}.
   */
  public boolean value() default true;
}
