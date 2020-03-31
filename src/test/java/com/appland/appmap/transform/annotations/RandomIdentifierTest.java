package com.appland.appmap.transform.annotations;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

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
