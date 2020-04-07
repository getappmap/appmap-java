package com.appland.appmap.process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * Single-threaded locking mechanisms. This class provides behavior to restrict hooks within a
 * single thread from running while another hook is already in progress.
 */
public class ThreadLock {
  private class ThreadLockStatus {
    public Boolean globalLock = false;
    public HashSet<String> uniqueKeys;

    ThreadLockStatus() { }

    public boolean addUniqueKey(String key) {
      if (this.uniqueKeys == null) {
        this.uniqueKeys = new HashSet<String>();
      }

      return this.uniqueKeys.add(key);
    }

    public boolean contains(String key) {
      if (this.uniqueKeys == null) {
        return false;
      }

      return this.uniqueKeys.contains(key);
    }

    public boolean hasGlobalLock() {
      return this.globalLock;
    }

    public void setGlobalLock(Boolean globalLock) {
      this.globalLock = globalLock;
    }
  }
  private static final HashMap<Long, ThreadLock> instances =
      new HashMap<Long, ThreadLock>();

  private final Stack<ThreadLockStatus> statusStack = new Stack<ThreadLockStatus>();
  private Boolean isLocked = false;

  private ThreadLock() { }

  /**
   * Get the ThreadLock instance for this thread.
   * @return A ThreadLock instance unique to the current thread
   */
  public static ThreadLock current() {
    Long threadId = Thread.currentThread().getId();
    ThreadLock instance = ThreadLock.instances.get(threadId);
    if (instance == null) {
      instance = new ThreadLock();
      ThreadLock.instances.put(threadId, instance);
    }
    return instance;
  }

  /**
   * Checks if any execution context holds the global lock.
   * @return {@code true} if the global lock is locked. Otherwise, {@code false}.
   */
  public boolean isLocked() {
    for (ThreadLockStatus status : this.statusStack) {
      if (status.hasGlobalLock()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if the current execution context is globally locked.
   * @return {@code true} if the global lock is locked. Otherwise, {@code false}.
   */
  public boolean hasLock() {
    if (this.statusStack.isEmpty()) {
      return false;
    }

    return this.statusStack.peek().hasGlobalLock();
  }

  /**
   * Checks if the current execution context holds a lock on a unique key.
   * @param key The unique key to check lock status on
   * @return {@code true} if the global lock is locked. Otherwise, {@code false}.
   */
  public boolean hasUniqueLock(String key) {
    if (this.statusStack.isEmpty()) {
      return false;
    }

    return this.statusStack.peek().contains(key);
  }

  /**
   * Pushes a new execution context on the stack. This should be called when first entering a
   * method, before acquiring any locks.
   * @return {@code true} if the global lock is locked. Otherwise, {@code false}.
   * @see ThreadLock#exit
   */
  public void enter() {
    this.statusStack.push(new ThreadLockStatus());
  }

  /**
   * Pops an execution context off the stack. This should be called just before exiting a method.
   * Releases any existing locks held.
   * @see ThreadLock#enter
   */
  public void exit() {
    if (this.statusStack.isEmpty()) {
      return;
    }

    this.statusStack.pop();
  }

  /**
   * Attempts to acquire a lock on a unique key for the current execution context.
   * @return {@code true} if the unique lock was acquired. Otherwise, {@code false}.
   */
  public boolean lockUnique(String key) {
    if (this.statusStack.isEmpty()) {
      return false;
    }

    for(ThreadLockStatus status : this.statusStack) {
      if (status.contains(key)) {
        return false;
      }
    }

    return this.statusStack.peek().addUniqueKey(key);
  }

  /**
   * Attempts to acquire the global lock as the current execution context.
   * @return {@code true} if the global lock was acquired. Otherwise, {@code false}.
   */
  public boolean lock() {
    if (this.isLocked()) {
      return false;
    }

    if (this.statusStack.isEmpty()) {
      return false;
    }

    this.statusStack.peek().setGlobalLock(true);
    return true;
  }

  /**
   * Attempts to release a global lock on the current execution context.
   * @return {@code true} if the lock was successfully released. Otherwise, {@code false}.
   */
  public boolean unlock() {
    if (this.statusStack.isEmpty()) {
      return false;
    }

    final ThreadLockStatus top = this.statusStack.peek();
    if (!top.hasGlobalLock()) {
      return false;
    }

    top.setGlobalLock(false);
    return true;
  }
}
