package com.appland.appmap.config;

import com.appland.appmap.util.FullyQualifiedName;
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
  public Boolean includes(FullyQualifiedName canonicalName) {
    if (this.path == null) {
      return false;
    }
    if (canonicalName == null) {
      return false;
    }

    if (!canonicalName.toString().startsWith(this.path)) {
      return false;
    }

    return !this.excludes(canonicalName);
  }

  /**
   * Returns whether or not the canonical name is explicitly excluded
   * @param canonicalName the canonical name of the class/method to be checked
   */
  public Boolean excludes(FullyQualifiedName canonicalName) {
    for (String exclusion : this.exclude) {
      if (canonicalName.toString().startsWith(exclusion)) {
        return true;
      }
    }

    return false;
  }
}
