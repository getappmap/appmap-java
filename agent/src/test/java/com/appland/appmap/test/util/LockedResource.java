package com.appland.appmap.test.util;

public class LockedResource {
  public synchronized void lockingMethod() throws InterruptedException {
    Thread.sleep(1000);
  }

  @Override
  public String toString() {
    synchronized (this) {
      return "hello world";
    }
  }
}
