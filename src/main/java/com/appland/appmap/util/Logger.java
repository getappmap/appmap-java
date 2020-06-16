package com.appland.appmap.util;

public class Logger {
  private static final Boolean debug = (System.getProperty("appmap.debug") != null);

  public static void println(Exception e) {
    Logger.println(e.getMessage());
  }

  public static void println(String msg) {
    if (!debug) {
      return;
    }

    System.err.println("AppMap [DEBUG]: " + msg);
  }

  public static void printf(String format, Object... args) {
    if (!debug) {
      return;
    }

    System.err.printf("AppMap [DEBUG]: " + format, args);
  }
}