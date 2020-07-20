package com.appland.appmap.test.util;

import java.util.ArrayList;

public class UnhandledExceptionCollection implements Thread.UncaughtExceptionHandler {
  private ArrayList<Throwable> exceptions = new ArrayList<Throwable>();

  public UnhandledExceptionCollection() {
  }

  public void uncaughtException(Thread th, Throwable ex) {
    this.exceptions.add(ex);
  }

  public Boolean contains(Class<? extends Throwable> clazz) {
    for (Throwable e : this.exceptions) {
      if (clazz.isInstance(e)) {
        return true;
      }
    }
    return false;
  }
}
