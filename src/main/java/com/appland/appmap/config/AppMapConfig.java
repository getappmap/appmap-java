package com.appland.appmap.config;

import com.appland.appmap.util.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AppMapConfig {
  public String name;
  public AppMapPackage[] packages = new AppMapPackage[0];
  private static AppMapConfig singleton = new AppMapConfig();
  private final Map<String, PackageConfig> packageConfigMap = new HashMap<>();

  /**
   * Populate the configuration from a file.
   * @param configFile The file to be loaded
   * @return The AppMapConfig singleton
   */
  public static AppMapConfig load(File configFile) {
    InputStream inputStream = null;

    try {
      inputStream = new FileInputStream(configFile);
    } catch (FileNotFoundException e) {
      Logger.println(String.format("error: file not found -> %s", configFile.getPath()));
      Logger.error(String.format("error: file not found -> %s", configFile.getPath()));
      return null;
    }

    Yaml yaml = new Yaml();

    try {
      singleton = yaml.loadAs(inputStream, AppMapConfig.class);
    } catch (YAMLException e) {
      Logger.error("AppMap: encountered syntax error in appmap.yml " + e.getMessage());
      System.exit(1);
    }
    
    return singleton;
  }

  /**
   * Get the AppMapConfig singleton.
   * @return The singleton instance
   */
  public static AppMapConfig get() {
    return singleton;
  }

  /**
   * Check if a class/method is included in the configuration.
   * @param canonicalName the canonical name of the class/method to be checked
   * @return {@code true} if the class/method is included in the configuration. {@code false} if it
   *         is not included or otherwise explicitly excluded.
   */
  public Boolean includes(String canonicalName) {
    if (this.packages == null) {
      return false;
    }

    for (AppMapPackage pkg : this.packages) {
      if (pkg.includes(canonicalName)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if a class/method is explicitly excluded in the configuration.
   * @param canonicalName the canonical name of the class/method to be checked
   * @return {@code true} if the class/method is explicitly excluded in the configuration. Otherwise, {@code false}.
   */
  public Boolean excludes(String canonicalName) {
    if (this.packages == null) {
      return false;
    }

    for (AppMapPackage pkg : this.packages) {
      if (pkg.excludes(canonicalName)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Retrieve configuration of a package from appmap.yml
   * @param packageName package name
   * @return configuration found in appmap.yml
   */
  public PackageConfig getPackageConfig(String packageName) {
    if ( packageName == null ) return null;

    if( packageConfigMap.containsKey(packageName) ) return packageConfigMap.get(packageName);

    String pkgName = null;
    boolean pkgShallow = false;

    //iterate over all configured paths and find the nearest parent package
    for (AppMapPackage pkg : this.packages) {
      if ( pkg.includes(packageName) ) {
        if( pkgName == null ) {
          pkgName = pkg.path;
          pkgShallow = pkg.shallow;
        } else if( pkg.path.length() > pkgName.length() ) {
          pkgName = pkg.path;
          pkgShallow = pkg.shallow;
        }
      }
    }

    PackageConfig config = null;
    //return null for packages that were not included in appmap.yml
    if ( pkgName != null ) {
      config = new PackageConfig(packageName, pkgName, pkgShallow);
    }
    packageConfigMap.put(pkgName, config);
    return config;
  }
}
