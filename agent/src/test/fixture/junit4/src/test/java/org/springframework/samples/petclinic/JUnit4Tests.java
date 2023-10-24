package org.springframework.samples.petclinic;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JUnit4Tests {
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
}
