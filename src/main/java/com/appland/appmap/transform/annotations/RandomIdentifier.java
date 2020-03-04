package com.appland.appmap.transform.annotations;

import java.security.SecureRandom;

public class RandomIdentifier {
  private static SecureRandom random = new SecureRandom();

  public static String build(String knownIdentifier) {
    final String randomHex = Long.toHexString(random.nextLong());
    return String.format("appmap$%s$%s", knownIdentifier, randomHex);
  }
}