package com.appland.appmap.transform.annotations;

import java.security.SecureRandom;

/**
 * Random identifiers for adding new local variables in existing methods.
 */
public class RandomIdentifier {
  private static SecureRandom random = new SecureRandom();

  /**
   * Builds a random identifier from a given input.
   * @param knownIdentifier A human readable description of the variable to be built
   * @return A variable identifier with 64 bits of randomness.
   */
  public static String build(String knownIdentifier) {
    final String randomHex = Long.toHexString(random.nextLong());
    return String.format("appmap$%s$%s", knownIdentifier, randomHex);
  }
}
