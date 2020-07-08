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
    final String canonicalName = className + (isStatic ? "." : "#") + methodName;

    if (this.path == null) {
      return false;
    }

    if (!canonicalName.startsWith(this.path)) {
      return false;
    }

    return !this.excludes(canonicalName);
  }

  /**
   * Returns whether or not the canonical name is explicitly excluded
   * @param canonicalName
   * @return
   */
  private Boolean excludes(String canonicalName) {
    for (String exclusion : this.exclude) {
      if (canonicalName.startsWith(exclusion)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if a class/method is explicitly excluded in the configuration.
   * @param className The class name to be checked
   * @param methodName The method name to be checked
   * @param isStatic {@code true} if the method is static
   * @return {@code true} if the class/method is explicitly excluded in the configuration. Otherwise, {@code false}.
   */
  public Boolean excludes(String className, String methodName, boolean isStatic) {
    final String canonicalName = className + (isStatic ? "." : "#") + methodName;
    return this.excludes(canonicalName);
  }
}
