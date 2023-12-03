package org.springframework.samples.petclinic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.appland.appmap.annotation.NoAppMap;

public class JUnit5Tests {
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

  @NoAppMap
  @Test
  public void testAnnotatedMethodNotRecorded() {
    System.err.println("passing annotated test, not recorded");

    assertTrue(true);
  }

  @Nested
  @NoAppMap
  class TestClass {
    @Test
    public void testAnnotatedClassNotRecorded() {
      System.err.println("passing annotated class, not recorded");

      assertTrue(true);
    }
  }

  @Test
  public void testWithParameter(@TempDir Path tempDir) throws IOException {
    assertTrue(Files.exists(tempDir));
  }
}
