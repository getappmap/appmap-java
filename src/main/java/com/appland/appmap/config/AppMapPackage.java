package com.appland.appmap.config;

public class AppMapPackage {
  public String path;
  public String[] exclude = new String[] {};

  /**
   * Check if a class/method is included in the configuration.
   * @param className The class name to be checked
   * @param methodName The method name to be checked
   * @param isStatic {@code true} if the method is static
   * @return {@code true} if the class/method is included in the configuration. {@code false} if it
   *         is not included or otherwise explicitly excluded.
   */
  public Boolean includes(String className, String methodName, boolean isStatic) {
    String canonicalName = className + (isStatic ? "." : "#") + methodName;

    if (this.path == null) {
      return false;
    }

    if (!canonicalName.startsWith(this.path)) {
      return false;
    }

    for (String exclusion : this.exclude) {
      if (canonicalName.startsWith(exclusion)) {
        return false;
      }
    }

    return true;
  }
}
