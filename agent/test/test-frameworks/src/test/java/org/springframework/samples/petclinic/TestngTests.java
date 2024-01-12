package org.springframework.samples.petclinic;

import org.testng.Assert;

import com.appland.appmap.annotation.NoAppMap;
import org.testng.annotations.Test;

public class TestngTests {
  @Test
  public void testItPasses() {
    System.err.println("passing test");

    Assert.assertTrue(true);
  }

  @Test
  public void testItFails() {
    System.err.println("failing test");

    Assert.assertTrue(false, "false is not true");
  }

  public static class TestNGException extends Exception {}

  @Test(expectedExceptions={TestNGException.class})
  public void testItThrows() throws Exception {
    System.err.println("test throwing expected exception");

    throw new TestNGException();
  }

  @NoAppMap
  @Test
  public void testAnnotatedMethodNotRecorded() {
    System.out.println("passing annotated test, not recorded");

    Assert.assertTrue(true);
  }

  @NoAppMap
  public static class TestClass {
    @Test
    public void testAnnotatedClassNotRecorded() {
      System.out.println("passing annotated class, not recorded");

      Assert.assertTrue(true);
    }
  }

}
