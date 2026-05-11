package com.appland.appmap.util;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.appland.appmap.config.AppMapConfig;

public class GitUtilTest {
  @TempDir
  Path tempDir;

  @Test
  public void testGetRepositoryURLNoRemotes() throws Exception {
    try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
      Path testFile = tempDir.resolve("test.txt");
      Files.write(testFile, "test".getBytes());
      git.add().addFilepattern("test.txt").call();
      git.commit().setMessage("initial commit").call();

      Path configFile = tempDir.resolve("appmap.yml");
      Files.write(configFile, "name: test\npackages: []".getBytes());

      AppMapConfig.load(configFile, true);

      try (GitUtil gitUtil = GitUtil.open()) {
        assertNull(gitUtil.getRepositoryURL(), "Repository URL should be null when no remotes exist");
      }
    }
  }
}
