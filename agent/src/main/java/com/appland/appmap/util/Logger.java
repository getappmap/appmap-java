package com.appland.appmap.util;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;

public class Logger {
  private static final org.tinylog.TaggedLogger logger = AppMapConfig.getLogger(null);
  
  public static void println(Throwable e) {
    logger.error(e);
  }

  public static void println(String msg) {
    logger.debug(msg);
  }

  public static void printf(String format, Object... args) {
    logger.debug(() -> String.format(format.trim(), args));
  }

  public static void error(String format, Object... args) {
    logger.error(() -> String.format(format.trim(), args));
  }

  public static void error(Throwable t) {
    println(t);
  }

  /**
   * Print a message on stderr unless the silent flag is set.
   * @param format
   * @param args
   */
  public static void printUserMessage(String format, Object... args) {
    if (!Properties.Silent) {
      System.err.printf(format, args);
    }
  }

}
