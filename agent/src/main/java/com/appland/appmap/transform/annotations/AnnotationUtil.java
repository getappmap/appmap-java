package com.appland.appmap.transform.annotations;

import static com.appland.appmap.util.ClassUtil.safeClassForName;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import javassist.CtBehavior;

/**
 * Utility classes for accessing runtime Annotation information.
 */
public class AnnotationUtil {
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
    return getObject(behavior, annotationClass, "value", defaultValue);
  }

  public static Integer getPosition(CtBehavior behavior,
                                Class<?> annotationClass,
                                Object defaultValue) {
    return (Integer)getObject(behavior, annotationClass, "position", defaultValue);
  }

  public static Object getObject(CtBehavior behavior,
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

  @SuppressWarnings("unchecked")
  public static boolean hasAnnotation(String annotationName, AnnotatedElement elt) {
    Class<?> annotation = safeClassForName(AnnotationUtil.class.getClassLoader(), annotationName);
    return annotation != null && elt.isAnnotationPresent((Class<? extends Annotation>) annotation);
  }
}
