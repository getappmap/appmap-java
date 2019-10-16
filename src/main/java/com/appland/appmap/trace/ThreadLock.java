package com.appland.appmap.trace;

import java.util.HashSet;

class ThreadLock {
  private HashSet<Long> lockedThreads = new HashSet<Long>();

  public Boolean tryLock() {
    Long threadId = Thread.currentThread().getId();
    Boolean isLocked = this.lockedThreads.contains(threadId);
    if (isLocked) {
      return false;
    }

    this.lockedThreads.add(threadId);
    return true;
  }

  public Boolean releaseLock() {
    Long threadId = Thread.currentThread().getId();
    Boolean isLocked = this.lockedThreads.contains(threadId);
    if (isLocked) {
      lockedThreads.remove(threadId);
    }
    return isLocked;
  }
}