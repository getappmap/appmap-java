package com.appland.appmap.config;

public class AppMapPackage {
  public String path;
  public String[] exclude = new String[] {};

  public Boolean includes(String className) {
    if (!className.startsWith(this.path)) {
      return false;
    }

    for (String exclusion : this.exclude) {
      if (className.startsWith(exclusion)) {
        return false;
      }
    }

    return true;
  }
}
