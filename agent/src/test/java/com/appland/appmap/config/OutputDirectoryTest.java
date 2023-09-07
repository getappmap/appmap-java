package com.appland.appmap.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

public class OutputDirectoryTest {
  private java.util.Properties origProperties;
  private FileSystem fs;
  private Path appDir;

  private Path configFile;

  @BeforeEach
  void saveProperties() throws IOException {
    origProperties = (java.util.Properties) System.getProperties().clone();
    String cwd = "/app";
    fs = MemoryFileSystemBuilder.newEmpty()
        .setCurrentWorkingDirectory(cwd)
        .build();
    appDir = fs.getPath(cwd);
    Files.createDirectories(appDir);

    String contents = "name: test\n";
    configFile = Files.createFile(appDir.resolve("appmap.yml"));
    Files.write(configFile, contents.getBytes());

    System.setProperty("appmap.config.file", configFile.toAbsolutePath().toString());
  }

  @AfterEach
  void restoreProperites() {
    System.setProperties(origProperties);
  }

  @Nested
  @DisplayName("When choosing an output directory")
  class WhenChoosingAnOutputDirectory {

    @Nested
    @DisplayName("When appmap.output.directory is set")
    class WhenPropertyIsSet {
      private final String EXPECTED_APPMAP_DIR = "/appmap_outdir";

      @BeforeEach
      void setOutputDirectory() {
        System.setProperty("appmap.output.directory", EXPECTED_APPMAP_DIR);
      }

      @Nested
      @DisplayName("When appmap_dir is set")
      class WhenConfigIsSet {

        private PrintStream mockedPrintStream;
        private PrintStream origStderr = System.err;

        @BeforeEach
        void beforeEach() throws Exception {
          mockedPrintStream = mock(PrintStream.class);
          System.setErr(mockedPrintStream);
        }

        @AfterEach
        void afterEach() {
          System.setErr(origStderr);
        }

        @Test
        @DisplayName("appmap_dir != appmap.output.directory")
        void configDoesntMatchProperty() throws Exception {
          final String contents = "appmap_dir: /not_appmap\n";
          Files.write(configFile, contents.getBytes());
          AppMapConfig.initialize(fs);

          assertEquals(EXPECTED_APPMAP_DIR, AppMapConfig.get().getAppmapDir().toString());

          String actualContents = new String(Files.readAllBytes(configFile));
          assertEquals(contents, actualContents);

          verify(mockedPrintStream).print(matches("appmap.yml contains appmap_dir"));
        }

        @Test
        @DisplayName("appmap_dir == appmap.output.directory")
        void configMatchesProperty() throws Exception {
          final String contents = String.format("appmap_dir: %s\n", EXPECTED_APPMAP_DIR);
          Files.write(configFile, contents.getBytes());
          AppMapConfig.initialize(fs);

          verifyNoInteractions(mockedPrintStream);
        }

      }
    }
  }

  @Nested
  @DisplayName("When appmap.output.directory is not set")
  class WhenPropertyIsNotSet {

    @BeforeEach
    void sanityCheck() {
      assertNull(System.getProperty("appmap.output.directory", null), "appmap.output.directory set?");
    }

    @Test
    void configContainsAppMapDir() throws Exception {
      String configDir = "/config/appmap";
      final String contents = "appmap_dir: " + configDir + "\n";
      Files.write(configFile, contents.getBytes());
      AppMapConfig.initialize(fs);

      assertEquals(configDir, AppMapConfig.get().getAppmapDir().toString());
    }

    @Test
    @DisplayName("the default is used")
    void defaultChosen() throws Exception {
      AppMapConfig.initialize(fs);

      assertEquals("tmp/appmap", AppMapConfig.get().getAppmapDir().toString());
    }

  }

}
