package com.appland.appmap.config;

import java.nio.file.Path;
import java.util.function.Function;

import com.appland.appmap.util.Logger;


public class Properties {
  public static final String APPMAP_OUTPUT_DIRECTORY_KEY = "appmap.output.directory";
  public static final Boolean Debug = (System.getProperty("appmap.debug") != null);
  public static final Boolean DebugHooks = Debug || (System.getProperty("appmap.debug.hooks") != null);
  public static final Boolean DebugLocals = (System.getProperty("appmap.debug.locals") != null);
  public static final Boolean DebugHttp = Debug || System.getProperty("appmap.debug.http") != null;
  public static final String DebugFile = resolveProperty("appmap.debug.file", (String)null);

  public static final Boolean RecordingAuto = resolveProperty(
      "appmap.recording.auto", Boolean::valueOf, false);
  public static final String RecordingName = resolveProperty(
      "appmap.recording.name", (String)null);
  public static final String RecordingFile = resolveProperty(
      "appmap.recording.file", (String)null);
  public static final Boolean RecordingRemote = resolveProperty(
      "appmap.recording.remote", Boolean::valueOf, true);
  public static final Boolean RecordingRequests = resolveProperty(
      "appmap.recording.requests", Boolean::valueOf, true);

  static Path OutputDirectory;

  public static final String DefaultConfigFile = "appmap.yml";
  public static final String ConfigFile = resolveProperty(
      "appmap.config.file", (String) null);

  public static final Integer DefaultMaxValueSize = 1024;
  public static final Integer MaxValueSize = resolveProperty(
      "appmap.event.valueSize", Integer::valueOf, DefaultMaxValueSize);

  public static final String[] DefaultRecords = new String[0];
  public static final String[] Records = resolveProperty(
      "appmap.record", DefaultRecords);

  public static final Boolean RecordPrivate = resolveProperty(
      "appmap.record.private", Boolean::valueOf, false);

  private static String resolveProperty(String propName, String defaultValue) {
    String value = defaultValue;
    try {
      final String propValue = System.getProperty(propName);
      if (propValue != null) {
        value = propValue;
      }
    } catch (Exception e) {
      Logger.printf("failed to resolve %s, falling back to default\n", propName);
      Logger.println(e);
    }
    return value;
  }

  public static Path getOutputDirectory() {
    if (OutputDirectory == null) {
      throw new InternalError("You must call AppMapConfig.initialize before using OutputDirectory");
    }

    return OutputDirectory;
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
      Logger.printf("failed to resolve %s, falling back to default\n", propName);
      Logger.println(e);

      value = defaultValue;
    }
    return value;
  }

  private static String[] resolveProperty(String propName, String[] defaultValue) {
    String[] value = defaultValue;
    try {
      final String propValue = System.getProperty(propName);
      if (propValue != null) {
        value = propValue.split(",");
        assert (value.length < 2);
      }
    } catch (Exception e) {
      // Logger may not be configured yet, so use System.err here to
      // report problems.
      System.err.printf("failed to resolve %s, falling back to default\n", propName);
      e.printStackTrace(System.err);
    }
    return value;
  }

  public static String[] getRecords() {
    return Records;
  }
}
