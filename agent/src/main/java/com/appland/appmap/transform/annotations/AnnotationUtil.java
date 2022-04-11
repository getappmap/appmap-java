package com.appland.appmap.transform.annotations;

import javassist.CtBehavior;

import java.lang.reflect.Method;

/**
 * Utility classes for accessing runtime Annotation information.
 */
class AnnotationUtil {
  /**
   * Obtains the default {@code value()} from an Annotation.
   * @param behavior The declaring behavior
   * @param annotationClass The annotation class
   * @param defaultValue A default value to use if an error occurs
   * @return The resulting value of the Annotations {@code value()} method or the default value
   *         supplied.
   */
  public static Object getValue(CtBehavior behavior,
                                Class<?> annotationClass,
                                Object defaultValue) {
    return getValue(behavior, annotationClass, "value", defaultValue);
  }

  public static Integer getPosition(CtBehavior behavior,
                                Class<?> annotationClass,
                                Object defaultValue) {
    return (Integer)getValue(behavior, annotationClass, "position", defaultValue);
  }

  private static Object getValue(CtBehavior behavior,
                                Class<?> annotationClass,
                                String annotationName,
                                Object defaultValue) {
    try {
      Object annotation = behavior.getAnnotation(annotationClass);
      if (annotation == null) {
        annotation = behavior.getDeclaringClass().getAnnotation(annotationClass);
      }

      if (annotation == null) {
        return defaultValue;
      }

      Method valueMethod = annotationClass.getMethod(annotationName);
      if (valueMethod == null) {
        return defaultValue;
      }

      return valueMethod.invoke(annotation);
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
