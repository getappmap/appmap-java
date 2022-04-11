package com.appland.appmap.config;

public class AppMapPackage {
  public String path;
  public String[] exclude = new String[] {};
  public boolean shallow = false;

  /**
   * Check if a class/method is included in the configuration.
   * @param canonicalName the canonical name of the class/method to be checked
   * @return {@code true} if the class/method is included in the configuration. {@code false} if it
   *         is not included or otherwise explicitly excluded.
   */
  public Boolean includes(String canonicalName) {
    if (this.path == null) {
      return false;
    }
    if (canonicalName == null) {
      return false;
    }

    if (!canonicalName.startsWith(this.path)) {
      return false;
    }

    return !this.excludes(canonicalName);
  }

  /**
   * Returns whether or not the canonical name is explicitly excluded
   * @param canonicalName the canonical name of the class/method to be checked
   */
  public Boolean excludes(String canonicalName) {
    for (String exclusion : this.exclude) {
      if (canonicalName.startsWith(exclusion)) {
        return true;
      }
    }

    return false;
  }
}
