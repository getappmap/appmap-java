package com.appland.appmap.config;

import com.appland.appmap.util.Logger;

import java.util.function.Function;

public class Properties {
  public static final Boolean Debug = (System.getProperty("appmap.debug") != null);
  public static final Boolean DebugHooks = (System.getProperty("appmap.debug.hooks") != null);
  public static final Boolean DebugLocals = (System.getProperty("appmap.debug.locals") != null);

  public static final String DefaultOutputDirectory = "./tmp/appmap";
  public static final String OutputDirectory = resolveProperty(
      "appmap.output.directory", DefaultOutputDirectory);

  public static final String DefaultConfigFile = "appmap.yml";
  public static final String ConfigFile = resolveProperty(
      "appmap.config.file", DefaultConfigFile);

  public static final Integer DefaultMaxValueSize = 1024;
  public static final Integer MaxValueSize = resolveProperty(
      "appmap.event.valueSize", Integer::valueOf, DefaultMaxValueSize);

  private static String resolveProperty(String propName, String defaultValue) {
    String value = defaultValue;
    try {
      final String propValue = System.getProperty(propName);
      if (propValue != null) {
        value = propValue;
      }
    } catch (Exception e) {
      Logger.printf("failed to resolve %f, falling back to default\n", propName);
      Logger.println(e);
    }
    return value;
  }

  private static <T> T resolveProperty(String propName,
                                       Function<String, T> resolvingFunc,
                                       T defaultValue) {
    T value = defaultValue;
    try {
      final String propValue = System.getProperty(propName);
      if (propValue != null) {
        value = resolvingFunc.apply(propValue);
      }
    } catch (Exception e) {
      Logger.printf("failed to resolve %f, falling back to default\n", propName);
      Logger.println(e);

      value = defaultValue;
    }
    return value;
  }
}
