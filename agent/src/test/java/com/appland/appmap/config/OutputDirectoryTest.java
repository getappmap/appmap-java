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

  @BeforeEach
  void saveProperties() throws IOException {
    origProperties = (java.util.Properties)System.getProperties().clone();
  }

  @AfterEach
  void restoreProperites() {
    System.setProperties(origProperties);
  }

  static class WithWorkingDirectory {
    FileSystem fs;
    Path appDir;
    Path configFile;

    String getCwd() {
      return "/app";
    }

    @BeforeEach
    void withWorkingDirectory() throws IOException {
      String cwd = getCwd();
      fs = MemoryFileSystemBuilder.newEmpty().setCurrentWorkingDirectory(cwd).build();
      appDir = fs.getPath("/app");
      Files.createDirectories(fs.getPath(cwd));

      String contents = "name: test\n";
      configFile = Files.createFile(appDir.resolve("appmap.yml"));
      Files.write(configFile, contents.getBytes());

      System.setProperty("appmap.config.file", configFile/* .toAbsolutePath() */.toString());
    }
  }

  @Nested
  @DisplayName("When choosing an output directory")
  class WhenChoosingAnOutputDirectory extends WithWorkingDirectory {

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

          assertEquals(EXPECTED_APPMAP_DIR, Properties.OutputDirectory.toString());

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

    @Nested
    @DisplayName("cwd is root directory")
    class InRootDirectory extends WithWorkingDirectory {
      @Test
      void configContainsAppMapDir() throws Exception {
        String outdir = "tmp/appmap";
        final String contents = "appmap_dir: " + outdir + "\n";
        Files.write(configFile, contents.getBytes());
        AppMapConfig.initialize(fs);

        Path expected = configFile.toAbsolutePath().getParent().resolve(outdir);
        assertEquals(expected.toString(), Properties.OutputDirectory.toString());
      }

      @Test
      @DisplayName("the default is used")
      void defaultChosen() throws Exception {
        AppMapConfig.initialize(fs);

        String expected = fs.getPath("tmp/appmap").toAbsolutePath().toString();
        assertEquals(expected, Properties.OutputDirectory.toString());
      }
    }
    @Nested
    @DisplayName("cwd is a subdirectory")
    class InSubdirectory extends WithWorkingDirectory {
      @Override
      public String getCwd() {
        return "/app/subdir";
      }

      @Test
      void relativeToConfig() throws Exception {
        AppMapConfig.initialize(fs);

        String expected = fs.getPath("/app/tmp/appmap").toAbsolutePath().toString();
        assertEquals(expected, Properties.OutputDirectory.toString());
      }
    }
  }

}
