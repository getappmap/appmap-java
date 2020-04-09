package com.appland.appmap.transform.annotations;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;

public class RandomIdentifierTest {
  private static final int ITERATIONS = 100000;

  @Test
  public void testAdequatelyRandom() {
    final Set<String> identifiers = new HashSet<String>();
    for (int i = 0; i < ITERATIONS; i++) {
      if (!identifiers.add(RandomIdentifier.build("someId"))) {
        fail("identifier collision on iteration #" + i);
      }
    }
  }
}
