package com.appland.appmap.config;

import java.util.regex.Pattern;
import com.appland.appmap.util.FullyQualifiedName;
import com.appland.appmap.util.Logger;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppMapPackage {
  public String path;
  public String[] exclude = new String[] {};
  public boolean shallow = false;
  public Boolean allMethods = true;

  public static class LabelConfig {
    private Pattern className = null;
    private Pattern name = null;

    private String[] labels = new String[] {};

    public LabelConfig() {}

    @JsonCreator
    public LabelConfig(@JsonProperty("class") String className, @JsonProperty("name") String name,
        @JsonProperty("labels") String[] labels) {
      this.className = Pattern.compile("\\A(" + className + ")\\z");
      this.name = Pattern.compile("\\A(" + name + ")\\z");
      this.labels = labels;
    }

    public String[] getLabels() {
      return this.labels;
    }

    public boolean matches(FullyQualifiedName name) {
      return matches(name.className, name.methodName);
    }

    public boolean matches(String className, String methodName) {
      return this.className.matcher(className).matches() && this.name.matcher(methodName).matches();
    }

  }

  public LabelConfig[] methods = null;

  /**
   * Check if a class/method is included in the configuration.
   * 
   * @param canonicalName the canonical name of the class/method to be checked
   * @return {@code true} if the class/method is included in the configuration. {@code false} if it
   *         is not included or otherwise explicitly excluded.
   */
  public LabelConfig find(FullyQualifiedName canonicalName) {
    if (this.path == null) {
      return null;
    }

    if (canonicalName == null) {
      return null;
    }

    // If no method configs are set, use the old matching behavior.
    if (this.methods == null) {
      if (!canonicalName.toString().startsWith(this.path)) {
        return null;
      }

      return this.excludes(canonicalName) ? null : new LabelConfig();
    }

    if (!canonicalName.packageName.equals(this.path)) {
      return null;
    }

    for (LabelConfig ls : this.methods) {
      if (ls.matches(canonicalName)) {
        return ls;
      }
    }

    return null;
  }

  /**
   * Returns whether or not the canonical name is explicitly excluded
   * 
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
