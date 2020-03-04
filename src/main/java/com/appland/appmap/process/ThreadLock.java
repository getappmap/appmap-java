package com.appland.appmap.process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class ThreadLock {
  private static final HashMap<Long, ThreadLock> instances =
      new HashMap<Long, ThreadLock>();

  private final HashSet<String> uniqueKeys = new HashSet<String>();
  private Boolean isLocked = false;

  private ThreadLock() { }

  public static ThreadLock current() {
    Long threadId = Thread.currentThread().getId();
    ThreadLock instance = ThreadLock.instances.get(threadId);
    if (instance == null) {
      instance = new ThreadLock();
      ThreadLock.instances.put(threadId, instance);
    }
    return instance;
  }

  public boolean isLocked(String key) {
    return this.isLocked || this.uniqueKeys.contains(key);
  }

  public boolean isLocked() {
    return this.isLocked;
  }

  public boolean releaseUniqueLock(String key) {
    return this.uniqueKeys.remove(key);
  }

  public boolean acquireUniqueLock(String key) {
    return this.uniqueKeys.add(key);
  }

  public boolean acquireLock() {
    this.isLocked = true;
    return true;
  }

  public boolean acquireLock(String uniqueKey) {
    if (this.isLocked(uniqueKey)) {
      return false;
    }

    this.acquireUniqueLock(uniqueKey);
    this.isLocked = true;
    return true;
  }

  public void releaseLock() {
    this.isLocked = false;
  }

  public void releaseLock(String uniqueKey) {
    this.isLocked = false;
    this.releaseUniqueLock(uniqueKey);
  }

  public void setLock(boolean val) {
    this.isLocked = val;
  }
}
