package com.appland.appmap.test.util;

public class MyThread extends Thread {
  private long id;

  public MyThread(long threadId, Runnable runnable) {
    super(runnable);
    this.id = threadId;
  }

  public MyThread(Runnable runnable) {
    super(runnable);
    this.id = (long) Math.ceil(Math.random() * Long.MAX_VALUE);
  }

  @Override
  public long getId() {
    return this.id;
  }
}
