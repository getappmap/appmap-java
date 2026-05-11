package com.appland.appmap.record;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.record.Recorder.Metadata;

public class AppMapSerializerTest {
  @TempDir
  Path tempDir;

  @BeforeEach
  public void setup() throws Exception {
    Path configFile = tempDir.resolve("appmap.yml");
    Files.write(configFile, "name: test\npackages: []".getBytes());
    AppMapConfig.load(configFile, true);
  }

  @Test
  public void testMetadataNoRepositoryURL() throws Exception {
    try (var gitRepo = Git.init().setDirectory(tempDir.toFile()).call()) {
      Path testFile = tempDir.resolve("test.txt");
      Files.write(testFile, "test".getBytes());
      gitRepo.add().addFilepattern("test.txt").call();
      gitRepo.commit().setMessage("initial commit").call();
    }

    Metadata metadata = new Metadata("test-recorder", "test-type");
    
    StringWriter writer = new StringWriter();
    try (AppMapSerializer serializer = AppMapSerializer.open(writer)) {
      serializer.write(new CodeObjectTree(), metadata, Map.of());
    }

    String json = writer.toString();
    assertFalse(json.contains("\"repository\""), "Metadata should not contain 'repository' if URL is null. JSON: " + json);
  }
}
