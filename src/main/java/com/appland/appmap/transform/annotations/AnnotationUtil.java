package com.appland.appmap.transform.annotations;

import java.lang.reflect.Method;
import javassist.CtBehavior;

class AnnotationUtil {
  public static Object getValue(CtBehavior behavior, Class<?> annotationClass, Object defaultValue) {
    try {
      Object annotation = behavior.getAnnotation(annotationClass);
      if (annotation == null) {
        annotation = behavior.getDeclaringClass().getAnnotation(annotationClass);
      }

      if (annotation == null) {
        return defaultValue;
      }

      Method valueMethod = annotationClass.getMethod("value");
      if (valueMethod == null) {
        return defaultValue;
      }

      return valueMethod.invoke(annotation);
    } catch (Exception e) {
      return defaultValue;
    }
  }
}