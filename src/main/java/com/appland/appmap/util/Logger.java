package com.appland.appmap.util;

import com.appland.appmap.config.Properties;

public class Logger {
  public static void println(Exception e) {
    Logger.println(e.getMessage());
  }

  public static void println(String msg) {
    if (!Properties.Debug) {
      return;
    }

    System.err.println("AppMap [DEBUG]: " + msg);
  }

  public static void printf(String format, Object... args) {
    if (!Properties.Debug) {
      return;
    }

    System.err.printf("AppMap [DEBUG]: " + format, args);
  }
}
