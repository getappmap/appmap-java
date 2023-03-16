package com.appland.appmap.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.appland.appmap.config.Properties;

public class Logger {
  private static PrintStream log = ensureLog();
  
  public static void println(Throwable e) {
    e.printStackTrace(log);
  }

  public static void println(String msg) {
    if (!Properties.Debug) {
      return;
    }

    log.println("AppMap[" + Thread.currentThread() + "] [DEBUG]: " + msg);
  }

  public static void printf(String format, Object... args) {
    if (!Properties.Debug) {
      return;
    }

    log.printf("AppMap[" + Thread.currentThread() + "] [DEBUG]: " + format, args);
  }

  public static void error(String format, Object... args) {
    System.err.printf("AppMap[" + Thread.currentThread() + "] [ERROR]: " + format, args);
  }

  public static void error(Throwable t) {
    t.printStackTrace();
  }
  public static void whereAmI(String msg) {
    if (!Properties.Debug) {
      return;
    }

    println(msg);
    new Exception().printStackTrace(log);
  }

  public static void whereAmI() {
    whereAmI("whereAmI?");
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
