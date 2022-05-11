package com.appland.appmap.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class FullyQualifiedNameTest {
  FullyQualifiedName fqn;

  @Test
  void testPackageNameIsNull() {
    fqn = new FullyQualifiedName(null,"class",false,"method");
    assertEquals("", fqn.packageName);
  };
}
