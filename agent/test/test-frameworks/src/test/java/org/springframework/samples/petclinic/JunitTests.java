package org.springframework.samples.petclinic;

import static org.junit.Assert.assertTrue;

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

}
