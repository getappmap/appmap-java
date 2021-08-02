package com.appland.appmap.util;

import com.appland.appmap.config.Properties;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Logger {
  private static PrintStream log = ensureLog();
  
  public static void println(Throwable e) {
    e.printStackTrace(log);
  }

  public static void println(String msg) {
    if (!Properties.Debug) {
      return;
    }

    log.println("AppMap [DEBUG]: " + msg);
  }

  public static void printf(String format, Object... args) {
    if (!Properties.Debug) {
      return;
    }

    log.printf("AppMap [DEBUG]: " + format, args);
  }

  public static void error(String msg) {
    System.err.println("AppMap [ERROR]: " + msg);
  }

  public static void whereAmI() {
    new Exception().printStackTrace(log);
  }
  
  private static PrintStream ensureLog() {
    final String debugFile = Properties.DebugFile;
    try {
      return debugFile != null? new PrintStream(new FileOutputStream(debugFile)) : System.err;
    }
    catch (IOException e) {
      System.err.printf("AppMap [DEBUG]: Warning, failed opening file: %s. Using System.err instead.\n", e.getMessage());
    }
    return System.err;
  }
}
