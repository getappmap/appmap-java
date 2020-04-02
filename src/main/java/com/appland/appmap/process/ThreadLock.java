package com.appland.appmap.process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * Single-threaded locking mechanisms. This class provides behavior to restrict hooks within a
 * single thread from running while another hook is already in progress.
 */
public class ThreadLock {
  private static final HashMap<Long, ThreadLock> instances =
      new HashMap<Long, ThreadLock>();

  private final HashSet<String> uniqueKeys = new HashSet<String>();
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
   * Checks if this ThreadLock is locked globally or with a unique key.
   * @param key A unique key
   * @return {@code true} if the global lock is locked or the unique key is in use. Otherwise,
   *         {@code false}.
   */
  public boolean isLocked(String key) {
    return this.isLocked || this.uniqueKeys.contains(key);
  }

  /**
   * Checks if this ThreadLock is locked.
   * @return {@code true} if the global lock is locked. Otherwise, {@code false}.
   */
  public boolean isLocked() {
    return this.isLocked;
  }

  /**
   * Remove a unique key from the locked list.
   * @param key The unique key to be removed
   * @return {@code true} if the unique key was previously locked. Otherwise, {@code false}.
   */
  public boolean releaseUniqueLock(String key) {
    return this.uniqueKeys.remove(key);
  }

  /**
   * Add a unique key to the locked list.
   * @param key The unique key to be removed
   * @return {@code true} if the unique key was locked. Otherwise, {@code false}.
   */
  public boolean acquireUniqueLock(String key) {
    return this.uniqueKeys.add(key);
  }

  /**
   * Locks the global lock, regardless of its current status.
   * @return {@code true}.
   */
  public boolean acquireLock() {
    this.isLocked = true;
    return true;
  }

  /**
   * Attempts to lock the global lock and a unique key.
   * @param uniqueKey The unique key to lock
   * @return {@code true} if both locks were acquired. Otherwise, {@code false}.
   */
  public boolean acquireLock(String uniqueKey) {
    if (this.isLocked(uniqueKey)) {
      return false;
    }

    this.acquireUniqueLock(uniqueKey);
    this.isLocked = true;
    return true;
  }

  /**
   * Releases the global lock, regardless of its status.
   */
  public void releaseLock() {
    this.isLocked = false;
  }

  /**
   * Releases the global lock and a unique key.
   * @param uniqueKey The unique key to unlock
   */
  public void releaseLock(String uniqueKey) {
    this.isLocked = false;
    this.releaseUniqueLock(uniqueKey);
  }

  /**
   * Set the lock.
   * @param val {@code true} if the global lock is on. Otherwise, {@code false}.
   */
  public void setLock(boolean val) {
    this.isLocked = val;
  }
}
