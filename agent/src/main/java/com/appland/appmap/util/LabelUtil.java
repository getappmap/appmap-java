package com.appland.appmap.util;

import java.lang.reflect.Method;

import javassist.CtAppMapClassType;
import javassist.CtBehavior;

/**
 * Reads the {@code @Labels} annotation from a {@link CtBehavior} by class name, avoiding a
 * compile-time dependency on {@code com.appland.appmap.annotation.Labels}. The annotation class
 * gets relocated by the agent's shadowing process, so a direct reference would not match the
 * annotation the user actually placed on their method.
 */
public final class LabelUtil {
  public static final String LABELS_CLASS = "com.appland.appmap.annotation.Labels";

  private LabelUtil() {}

  public static boolean hasLabelAnnotation(CtBehavior behavior) {
    try {
      return behavior.hasAnnotation(LABELS_CLASS);
    } catch (Exception e) {
      Logger.println(e);
      return false;
    }
  }

  /**
   * @return the {@code value()} of the {@code @Labels} annotation on the given behavior, or
   *         {@code null} if the annotation is not present or cannot be read.
   */
  public static String[] readAnnotationLabels(CtBehavior behavior) {
    try {
      if (!behavior.hasAnnotation(LABELS_CLASS)) {
        return null;
      }
      Object annotation = CtAppMapClassType.getAnnotation(behavior, LABELS_CLASS);
      Method value = annotation.getClass().getMethod("value");
      return (String[])(value.invoke(annotation));
    } catch (Exception e) {
      Logger.println(e);
      return null;
    }
  }
}
