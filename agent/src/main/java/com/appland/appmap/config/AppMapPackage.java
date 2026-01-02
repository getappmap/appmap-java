package com.appland.appmap.config;

import static com.appland.appmap.util.ClassUtil.safeClassForName;

import java.util.regex.Pattern;

import org.tinylog.TaggedLogger;

import com.appland.appmap.transform.annotations.CtClassUtil;
import com.appland.appmap.util.FullyQualifiedName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javassist.CtBehavior;
public class AppMapPackage {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static String tracePrefix = Properties.DebugClassPrefix;

  public String path;
  public String[] exclude = new String[] {};
  public boolean shallow = false;
  public Boolean allMethods = true;

  @JsonCreator
  public AppMapPackage(@JsonProperty("path") String path,
                         @JsonProperty("exclude") String[] exclude,
                         @JsonProperty("shallow") Boolean shallow,
                         @JsonProperty("allMethods") Boolean allMethods) {
    this.path = path;
    this.exclude = exclude == null ? new String[] {} : exclude;
    this.shallow = shallow != null && shallow;
    this.allMethods = allMethods == null ? true : allMethods;
  }

  public static class LabelConfig {

    private Pattern className = null;
    private Pattern name = null;

    private String[] labels = new String[] {};
    private Class<?> cls;

    public LabelConfig() {}

    @JsonCreator
    public LabelConfig(@JsonProperty("class") String className, @JsonProperty("name") String name,
        @JsonProperty("labels") String[] labels) {
      this.className = Pattern.compile("\\A(" + className + ")\\z");
      this.cls = safeClassForName(Thread.currentThread().getContextClassLoader(), className);
      logger.trace("this.cls: {}", this.cls);
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
      boolean traceClass = tracePrefix == null || className.startsWith(tracePrefix);
      Class<?> cls = safeClassForName(Thread.currentThread().getContextClassLoader(), className);

      if (traceClass) {
        logger.trace("this.cls: {} cls: {}, isChildOf?: {}", this.cls, cls, CtClassUtil.isChildOf(cls, this.cls));
      }

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
    String className = canonicalName != null ? canonicalName.getClassName() : null;
    boolean traceClass = tracePrefix == null || className.startsWith(tracePrefix);
    if (traceClass) {
      logger.trace(canonicalName);
    }

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
   * Checks whether the behavior is explicitly excluded
   *
   * @param behavior the behavior to be checked
   * @return {@code true} if the behavior is excluded
   */
  public Boolean excludes(CtBehavior behavior) {
    final String fqClass = behavior.getDeclaringClass().getName();
    String candidateName = null;
    for (String exclusion : this.exclude) {
      if (fqClass.startsWith(exclusion)) {
        return true;
      } else {
        if (candidateName == null) {
          candidateName = fqClass + "." + behavior.getName();
        }

        if (candidateName.startsWith(exclusion.replace('#', '.'))) {
          return true;
        }
      }
    }

    return false;
  }

  public Boolean excludes(FullyQualifiedName canonicalName) {
    for (String exclusion : this.exclude) {
      if (canonicalName.toString().startsWith(exclusion)) {
        return true;
      }
    }

    return false;
  }
}
