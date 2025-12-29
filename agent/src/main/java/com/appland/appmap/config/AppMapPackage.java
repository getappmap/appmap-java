package com.appland.appmap.config;

import java.util.regex.Pattern;

import org.tinylog.TaggedLogger;

import com.appland.appmap.util.FullyQualifiedName;
import com.appland.appmap.util.PrefixTrie;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javassist.CtBehavior;

/**
 * Represents a package configuration for AppMap recording.
 *
 * <p>
 * Configuration modes (mutually exclusive):
 * <ul>
 * <li><b>Exclude mode:</b> When {@code methods} is null, records all methods in
 * the package
 * except those matching {@code exclude} patterns.</li>
 * <li><b>Methods mode:</b> When {@code methods} is set, records only methods
 * matching the
 * specified patterns. The {@code exclude} field is ignored in this mode.</li>
 * </ul>
 *
 * @see <a href=
 *      "https://appmap.io/docs/reference/appmap-java.html#configuration">AppMap
 *      Java Configuration</a>
 */
public class AppMapPackage {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static String tracePrefix = Properties.DebugClassPrefix;

  public String path;
  public final String packagePrefix;
  public String[] exclude = new String[] {};
  public boolean shallow = false;
  private final PrefixTrie excludeTrie = new PrefixTrie();

  @JsonCreator
  public AppMapPackage(@JsonProperty("path") String path,
      @JsonProperty("exclude") String[] exclude,
      @JsonProperty("shallow") Boolean shallow,
      @JsonProperty("methods") LabelConfig[] methods) {
    this.path = path;
    this.exclude = exclude == null ? new String[] {} : exclude;
    this.shallow = shallow != null && shallow;
    this.methods = methods;
    this.packagePrefix = this.path == null ? "!!dummy!!" : this.path + ".";

    // Warn if both exclude and methods are specified (methods takes precedence)
    if (exclude != null && exclude.length > 0 && methods != null && methods.length > 0) {
      logger.warn("Package '{}': both 'exclude' and 'methods' are specified. " +
          "The 'exclude' field will be ignored when 'methods' is set.", path);
    }

    // Build the exclusion trie only if we're in exclude mode
    if (exclude != null && methods == null) {
      for (String exclusion : exclude) {
        // Allow exclusions to use both '.' and '#' as separators
        // for backward compatibility
        exclusion = exclusion.replace('#', '.');
        if (exclusion.startsWith(this.packagePrefix)) {
          // Absolute path: strip the package prefix
          this.excludeTrie.insert(exclusion.substring(this.packagePrefix.length()));
        } else {
          // Relative path: use as-is
          this.excludeTrie.insert(exclusion);
        }
      }
    }
  }

  /**
   * Configuration for matching specific methods with labels.
   * Used in "methods mode" to specify which methods to record.
   */
  public static class LabelConfig {
    private Pattern className = null;
    private Pattern name = null;
    private String[] labels = new String[] {};

    /** Empty constructor for exclude mode (no labels). */
    public LabelConfig() {}

    @JsonCreator
    public LabelConfig(@JsonProperty("class") String className,
            @JsonProperty("name") String name,
        @JsonProperty("labels") String[] labels) {
      // Anchor patterns to match whole symbols only
      this.className = Pattern.compile("\\A(" + className + ")\\z");
      this.name = Pattern.compile("\\A(" + name + ")\\z");
      this.labels = labels;
    }

    public String[] getLabels() {
      return this.labels;
    }

    /**
     * Checks if the given fully qualified name matches this configuration.
     * Supports matching against both simple and fully qualified class names for
     * flexibility.
     *
     * @param fqn the fully qualified name to check
     * @return true if the patterns match
     */
    public boolean matches(FullyQualifiedName fqn) {
      // Try matching with simple class name (package-relative)
      if (matches(fqn.className, fqn.methodName)) {
        return true;
      }

      // Also try matching with fully qualified class name for better UX
      String fullyQualifiedClassName = fqn.getClassName();
      return matches(fullyQualifiedClassName, fqn.methodName);
    }

    /**
     * Checks if the given class name and method name match this configuration.
     *
     * @param className  the class name (simple or fully qualified)
     * @param methodName the method name
     * @return true if both patterns match
     */
    public boolean matches(String className, String methodName) {
      return this.className.matcher(className).matches()
          && this.name.matcher(methodName).matches();
    }
  }

  public LabelConfig[] methods = null;

  /**
   * Determines if a class/method should be recorded based on this package
   * configuration.
   *
   * <p>
   * Behavior depends on configuration mode:
   * <ul>
   * <li><b>Exclude mode</b> ({@code methods} is null): Returns a LabelConfig for
   * methods
   * in this package that are not explicitly excluded.</li>
   * <li><b>Methods mode</b> ({@code methods} is set): Returns a LabelConfig only
   * for methods
   * that match the specified patterns. The {@code exclude} field is ignored.</li>
   * </ul>
   *
   * @param canonicalName the fully qualified name of the method to check
   * @return the label config if the method should be recorded, or null otherwise
   */
  public LabelConfig find(FullyQualifiedName canonicalName) {
    // Early validation
    if (this.path == null || canonicalName == null) {
      return null;
    }

    // Debug logging
    if (tracePrefix == null || canonicalName.getClassName().startsWith(tracePrefix)) {
      logger.trace("Checking {}", canonicalName);
    }

    if (isExcludeMode()) {
      return findInExcludeMode(canonicalName);
    } else {
      return findInMethodsMode(canonicalName);
    }
  }

  /**
   * Checks if this package is configured in exclude mode (records everything
   * except exclusions).
   */
  private boolean isExcludeMode() {
    return this.methods == null;
  }

  /**
   * Finds a method in exclude mode: match if in package and not excluded.
   */
  private LabelConfig findInExcludeMode(FullyQualifiedName canonicalName) {
    String canonicalString = canonicalName.toString();

    // Check if the method is in this package or a subpackage
    if (!canonicalString.startsWith(this.path)) {
      return null;
    } else if (canonicalString.length() > this.path.length()) {
      // Must either equal the path exactly or start with "path." or "path#"
      // The "#" check is needed for unnamed packages
      // or when path specifies a class name
      final char nextChar = canonicalString.charAt(this.path.length());
      if (nextChar != '.' && nextChar != '#') {
        return null;
      }
    }

    // Check if it's explicitly excluded
    if (this.excludes(canonicalName)) {
      return null;
    }

    // Include it (no labels in exclude mode)
    return new LabelConfig();
  }

  /**
   * Finds a method in methods mode: match only if it matches a configured
   * pattern.
   */
  private LabelConfig findInMethodsMode(FullyQualifiedName canonicalName) {
    // Must be in the exact package (not subpackages)
    if (!canonicalName.packageName.equals(this.path)) {
      return null;
    }

    // Check each method pattern
    for (LabelConfig config : this.methods) {
      if (config.matches(canonicalName)) {
        return config;
      }
    }

    return null;
  }

  /**
   * Converts a fully qualified class name to a package-relative name.
   * For example, "com.example.foo.Bar" with package "com.example" becomes
   * "foo.Bar".
   *
   * @param fqcn the fully qualified class name
   * @return the relative class name, or the original if it doesn't start with the
   *         package prefix
   */
  private String getRelativeClassName(String fqcn) {
    if (fqcn.startsWith(this.packagePrefix)) {
      return fqcn.substring(this.packagePrefix.length());
    }
    return fqcn;
  }

  /**
   * Checks whether a behavior is explicitly excluded by this package
   * configuration.
   * Only meaningful in exclude mode; in methods mode, use {@link #find} instead.
   *
   * @param behavior the behavior to check
   * @return true if the behavior matches an exclusion pattern
   */
  public Boolean excludes(CtBehavior behavior) {
    String fqClass = behavior.getDeclaringClass().getName();
    String relativeClassName = getRelativeClassName(fqClass);

    // Check if the class itself is excluded
    if (this.excludeTrie.startsWith(relativeClassName)) {
      return true;
    }

    // Check if the specific method is excluded
    String methodName = behavior.getName();
    String relativeMethodPath = String.format("%s.%s", relativeClassName, methodName);
    return this.excludeTrie.startsWith(relativeMethodPath);
  }

  /**
   * Checks whether a fully qualified method name is explicitly excluded.
   * Only meaningful in exclude mode; in methods mode, use {@link #find} instead.
   *
   * @param canonicalName the fully qualified method name
   * @return true if the method matches an exclusion pattern
   */
  public Boolean excludes(FullyQualifiedName canonicalName) {
    String fqcn = canonicalName.toString();
    String relativeName = getRelativeClassName(fqcn);
    // Convert # to . to match the format stored in the trie
    relativeName = relativeName.replace('#', '.');
    return this.excludeTrie.startsWith(relativeName);
  }
}
