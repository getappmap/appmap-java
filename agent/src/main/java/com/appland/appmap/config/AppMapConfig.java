package com.appland.appmap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import com.appland.appmap.util.FullyQualifiedName;
import com.appland.appmap.util.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppMapConfig {
  public File configFile;  // the configFile used
  public String name;
  public AppMapPackage[] packages = new AppMapPackage[0];
  private static AppMapConfig singleton = new AppMapConfig();

  static File findConfig(File configFile) throws FileNotFoundException {
    if (configFile.exists()) {
      return configFile;
    }
    Path parent = configFile.toPath().toAbsolutePath().getParent();
    while (parent != null) {
      Path c = parent.resolve("appmap.yml");
      if (Files.exists(c)) {
        return c.toFile();
      }
      parent = parent.getParent();
    }

    throw new FileNotFoundException(configFile.toString());
  }

  /**
   * Populate the configuration from a file.
   * @param configFile The file to be loaded
   * @return The AppMapConfig singleton
   */
  public static AppMapConfig load(File configFile) {
    InputStream inputStream = null;

    try {
      configFile = AppMapConfig.findConfig(configFile);
      Logger.println(String.format("using config file -> %s",
                                   configFile.getAbsolutePath()));
      inputStream = new FileInputStream(configFile);
    } catch (FileNotFoundException e) {
      String expectedConfig = configFile.getAbsolutePath();
      Logger.println(String.format("error: file not found -> %s",
                                   expectedConfig));
      Logger.error(String.format("error: file not found -> %s",
                                 expectedConfig));
      return null;
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      singleton = mapper.readValue(inputStream, AppMapConfig.class);
    } catch (IOException e) {
      Logger.error("AppMap: encountered syntax error in appmap.yml " + e.getMessage());
      System.exit(1);
    }
    singleton.configFile = configFile;

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
  public Boolean includes(FullyQualifiedName canonicalName) {
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
  public Boolean excludes(FullyQualifiedName canonicalName) {
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

  public boolean isShallow(FullyQualifiedName canonicalName) {
    if (canonicalName == null) {
      return false;
    }
    for (AppMapPackage pkg : this.packages) {
      if (pkg.includes(canonicalName)) {
        return pkg.shallow;
      }
    }

    return false;
  }
}
