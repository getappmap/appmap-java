package com.appland.appmap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

public class AppMapConfig {
  public String name;
  public AppMapPackage[] packages;

  public static AppMapConfig load(File configFile) {
    InputStream inputStream = null;

    try {
      inputStream = new FileInputStream(configFile);
    } catch (FileNotFoundException e) {
      System.err.println(String.format("error: file not found -> %s", configFile.getPath()));
      return null;
    }

    Yaml yaml = new Yaml();
    return yaml.loadAs(inputStream, AppMapConfig.class);
    // return yaml.<AppMapConfig>load(inputStream);
  }
}
