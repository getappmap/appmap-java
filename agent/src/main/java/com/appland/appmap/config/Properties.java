package com.appland.appmap.config;

import java.nio.file.Path;
import java.util.function.Function;

import com.appland.appmap.util.Logger;


public class Properties {
  public static final String APPMAP_OUTPUT_DIRECTORY_KEY = "appmap.output.directory";
  public static final String DISABLE_LOG_FILE_KEY = "appmap.disableLogFile";
  public static final Boolean DisableLogFile = resolveProperty(DISABLE_LOG_FILE_KEY, true);
  public static final Boolean Debug = resolveProperty("appmap.debug", false);
  public static final Boolean DebugHooks = Debug || (System.getProperty("appmap.debug.hooks") != null);
  public static final Boolean DebugLocals = (System.getProperty("appmap.debug.locals") != null);
  public static final Boolean DebugHttp = Debug || System.getProperty("appmap.debug.http") != null;
  public static final String DebugFile = resolveProperty("appmap.debug.file", (String)null);
  public static final String DebugClassPrefix = resolveProperty("appmap.debug.classPrefix", (String) null);
  public static final Boolean SaveInstrumented =
      resolveProperty("appmap.debug.saveInstrumented", false);
  public static final Boolean DisableGit = resolveProperty("appmap.debug.disableGit", false);

  public static final Boolean RecordingAuto = resolveProperty("appmap.recording.auto", false);
  public static final String RecordingName = resolveProperty("appmap.recording.name", (String) null);
  public static final String RecordingFile = resolveProperty("appmap.recording.file", (String) null);
  public static final Boolean RecordingRemote = resolveProperty("appmap.recording.remote", true);
  public static final Boolean RecordingRequests = resolveProperty("appmap.recording.requests", true);
  public static final String[] IgnoredPackages =
      resolveProperty("appmap.recording.ignoredPackages", new String[] {"java.", "jdk.", "sun."});


  public static final String DefaultConfigFile = "appmap.yml";
  public static final String ConfigFile = resolveProperty("appmap.config.file", (String) null);
  public static final Integer PatternThreshold =
      resolveProperty("appmap.config.patternThreshold", 10);

  public static final Boolean DisableValue = resolveProperty("appmap.event.disableValue", false);
  public static final Integer MaxValueSize = resolveProperty("appmap.event.valueSize", 1024);

  public static final String[] Records = resolveProperty("appmap.record", new String[0]);
  public static final Boolean RecordPrivate = resolveProperty("appmap.record.private", false);


  static Path OutputDirectory;

  public static Path getOutputDirectory() {
    if (OutputDirectory == null) {
      throw new InternalError("You must call AppMapConfig.initialize before using OutputDirectory");
    }

    return OutputDirectory;
  }

  static Boolean resolveProperty(String propName, Boolean defaultValue) {
    return resolveProperty(propName, Boolean::valueOf, defaultValue);
  }

  static Integer resolveProperty(String propName, Integer defaultValue) {
    return resolveProperty(propName, Integer::valueOf, defaultValue);
  }

  static String resolveProperty(String propName, String defaultValue) {
    return resolveProperty(propName, Object::toString, defaultValue);
  }

  static <T> T resolveProperty(String propName,
                                       Function<String, T> resolvingFunc,
                                       T defaultValue) {
    T value = defaultValue;
    try {
      String envVar = propName.toUpperCase().replace('.', '_');
      String envVal = System.getenv(envVar);
      String propVal = System.getProperty(propName);
      String propValue = propVal != null ? propVal : envVal;
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

  static String[] resolveProperty(String propName, String[] defaultValue) {
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
