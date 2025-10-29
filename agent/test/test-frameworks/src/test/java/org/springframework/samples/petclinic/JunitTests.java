package org.springframework.samples.petclinic;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import com.appland.appmap.annotation.NoAppMap;
import org.junit.Test;

public class JunitTests {
  @Test
  public void testItPasses() {
    System.err.println("passing test");

    assertTrue(true);
  }

  @Test
  public void testItFails() {
    System.err.println("failing test");

    assertTrue("false is not true", false);
  }

  @NoAppMap
  @Test
  public void testAnnotatedMethodNotRecorded() {
    System.out.println("passing annotated test, not recorded");

    assertTrue(true);
  }

  @NoAppMap
  public static class TestClass {
    @Test
    public void testAnnotatedClassNotRecorded() {
      System.out.println("passing annotated class, not recorded");

      assertTrue(true);
    }
  }

  private static class ExecutorRunner {
    public Throwable run() {
      ExecutorService executor = newSingleThreadExecutor();
      Future<?> future = executor.submit(() -> {
        throw new RuntimeException("Off-thread exception for testing");
      });
      try {
        future.get();
      } catch (java.util.concurrent.ExecutionException e) {
        return e.getCause();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        executor.shutdown();
      }
      return null;
    }
  }

  @Test
  public void offThreadExceptionTest() throws Throwable {
    Throwable throwable = new ExecutorRunner().run();
    if (throwable == null) {
      throw new AssertionError("Expected exception from off-thread execution");
    }
    throw throwable;
  }
}
