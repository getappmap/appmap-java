package com.appland.appmap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import com.appland.appmap.test.util.MyClass;
import com.appland.appmap.test.util.MyThread;
import com.appland.appmap.test.util.UnhandledExceptionCollection;

import org.junit.Test;

public class ThreadTest {
  @Test
  public void testHookingThreadSubclassDoesNotOverflowStack()
      throws InterruptedException {
    final MyThread t = new MyThread(() -> {
      MyClass myClass = new MyClass();
      myClass.myMethod();
    });

    UnhandledExceptionCollection exceptions = new UnhandledExceptionCollection();
    t.setUncaughtExceptionHandler(exceptions);

    t.start();
    t.join();

    assertFalse(exceptions.contains(StackOverflowError.class));
  }

  @Test
  public void testConcurrentModificationNonUniqueThreadId()
      throws InterruptedException {
    final Runnable r = () -> {
      final MyClass myClass = new MyClass();

      for (int i = 0; i < 1000; ++i) {
        myClass.myMethod();
      }
    };

    final long sharedThreadId = 100;
    final int  numThreads = 8;
    final UnhandledExceptionCollection exceptions = new UnhandledExceptionCollection();
    final ArrayList<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < numThreads; ++i) {
      final MyThread t = new MyThread(sharedThreadId, r);
      t.setUncaughtExceptionHandler(exceptions);
      threads.add(t);
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }

    assertFalse(exceptions.contains(ConcurrentModificationException.class));
  }
}
