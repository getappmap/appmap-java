package com.appland.appmap.util;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import com.appland.appmap.config.Properties;

public class Logger {
  private static PrintStream log = ensureLog();
  
  public static void println(Exception e) {
    Logger.println(e.getMessage());
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

  private static PrintStream ensureLog() {
    try {
      return new PrintStream(new FileOutputStream("appmap-java.log"));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
