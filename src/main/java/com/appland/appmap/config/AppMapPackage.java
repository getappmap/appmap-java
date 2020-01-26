package com.appland.appmap.config;

public class AppMapPackage {
  public String path;
  public String[] exclude = new String[] {};

  public Boolean includes(String className, String methodName, boolean isStatic) {
    String canonicalName = String.join("", new String[]{ className, isStatic ? "." : "#", methodName });

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
