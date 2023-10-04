package org.springframework.samples.petclinic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class Junit5Tests {
  @Test
  public void testItPasses() {
    System.err.println("passing test");
    
    assertTrue(true);
  }

  @Test
  public void testItFails() {
    System.err.println("failing test");

    assertTrue(false);
  }
}
