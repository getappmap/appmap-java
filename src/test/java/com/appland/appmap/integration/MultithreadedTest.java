package com.appland.appmap.integration;

import com.appland.appmap.test.util.LockedResource;

import org.junit.Test;

public class MultithreadedTest {
  @Test(timeout = 15000)
  public void testLockedResourceValue() throws InterruptedException {
    final int iterations = 5;
    LockedResource lockedResource = new LockedResource();

    Thread t = new Thread(() -> {
      try {
        for (int i = 0; i < iterations; ++i) {
          lockedResource.lockingMethod();
        }
      } catch (InterruptedException e) {
        // end scope
      }
    });

    t.start();
    for (int i = 0; i < iterations; ++i) {
      lockedResource.lockingMethod();
    }
    t.join();
  }
}
