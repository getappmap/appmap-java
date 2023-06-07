package com.appland.appmap.util;

import com.appland.appmap.config.AppMapConfig;

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

}
