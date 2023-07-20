package com.appland.appmap.util;

public class ClassUtil {
  public static Class<?> safeClassForName(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
    }
    return null;
  }
}
