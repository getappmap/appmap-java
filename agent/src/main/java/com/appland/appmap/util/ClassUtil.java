package com.appland.appmap.util;

public class ClassUtil {
  public static Class<?> safeClassForName(String name) {
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      return cl.loadClass(name);
    } catch (ClassNotFoundException e) {
    }
    return null;
  }
}
