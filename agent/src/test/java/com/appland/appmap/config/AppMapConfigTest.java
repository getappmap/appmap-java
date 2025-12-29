package com.appland.appmap.config;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public class AppMapConfigTest {

    @TempDir
    Path tmpdir;

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void loadBadDirectory() throws Exception {
        // Trying to load appmap.yml in non-existent directory (that can't be created)
        // shouldn't work.
        Path badDir = Paths.get("/no-such-dir");
        assertFalse(Files.exists(badDir));
        String actualErr = tapSystemErr(() -> AppMapConfig.load(badDir.resolve("appmap.yml"), false));
        assertNotNull(actualErr.toString());
        assertTrue(actualErr.contains("file not found"));
    }

    @Test
    public void createDefault() throws IOException {
        // Just verify that the file gets created when it should. The contents
        // get verified elsewhere.
        Path configFile = tmpdir.resolve("appmap.yml");
        Files.deleteIfExists(configFile);
        AppMapConfig.load(configFile, false);
        assertTrue(Files.exists(configFile));
    }

    @Test
    public void preservesExisting() throws IOException {
        Path tmpConfigFile = Files.createTempFile("appmap-config.", ".yml");
        String expectedName = "not-a-real-app";
        // This isn't the name in the default config
        Files.write(tmpConfigFile, String.format("name: %s\n", expectedName).getBytes());
        AppMapConfig actual = AppMapConfig.load(tmpConfigFile, false);
        assertEquals(expectedName, actual.name);
    }

    @Test
    public void requiresExisting() throws Exception {
        Path configFile = tmpdir.resolve("appmap.yml");
        Files.deleteIfExists(configFile);

        String actualErr = tapSystemErr(() -> AppMapConfig.load(configFile, true));
        assertNotNull(actualErr.toString());
        assertTrue(actualErr.contains("file not found"));
    }

    /**
     * Ensure that a relative path to an existing candidate config gets resolved to an absolute
     * path.
     */
    @Test
    public void resolvesRelative() {
      System.err.println(System.getProperty("user.dir"));
      Path f = Paths.get("appmap.yml");
      AppMapConfig.load(f, false);
      Path expected = Paths.get("appmap.yml").toAbsolutePath();
      assertTrue(Files.exists(expected)); // sanity check
      assertEquals(expected, AppMapConfig.get().configFile);
    }

    /**
     * Check that if a non-existent config file in a subdirectory is specified, the config file in a
     * parent directory will be used. Also checks that the resulting config file path is absolute.
     */
    @Test
    public void loadParent() {
        System.err.println(System.getProperty("user.dir"));
        Path f = Paths.get("test", "appmap.yml");
        assertFalse(Files.exists(f));
        AppMapConfig.load(f, false);
        Path expected = Paths.get("appmap.yml").toAbsolutePath();
        assertTrue(Files.exists(expected)); // sanity check
        assertEquals(expected, AppMapConfig.get().configFile);
    }

    @Test
    public void hasAppmapDir() throws Exception {
        Path configFile = tmpdir.resolve("appmap.yml");
        final String contents = "appmap_dir: /appmap\n";
        Files.write(configFile, contents.getBytes());
        AppMapConfig.load(configFile, true);
        assertEquals("/appmap", AppMapConfig.get().getAppmapDir());
    }

    @Test
    public void loadPackagesKeyWithMissingValue() throws Exception {
        Path configFile = tmpdir.resolve("appmap.yml");
        final String contents = "name: test\npackages:\npath: xyz";
        Files.write(configFile, contents.getBytes());
        String actualErr = tapSystemErr(() -> AppMapConfig.load(configFile, false));
        assertTrue(actualErr.contains("AppMap: missing value for the 'packages'"));
    }

    @Test
    public void loadPackagesKeyWithScalarValue() throws Exception {
        Path configFile = Files.createTempFile("appmap", ".yml");
        final String contents = "name:q test\npackages: abc\n";
        Files.write(configFile, contents.getBytes());
        String actualErr = tapSystemErr(() -> AppMapConfig.load(configFile, false));
        assertTrue(actualErr.contains("AppMap: encountered syntax error in appmap.yml"));
    }

    @Test
    public void loadEmptyExcludeField() throws Exception {
        Path configFile = tmpdir.resolve("appmap.yml");
        final String contents = "name: test\npackages:\n- path: com.example\n  exclude:\n";
        Files.write(configFile, contents.getBytes());
        
        AppMapConfig config = AppMapConfig.load(configFile, false);
        assertNotNull(config);
        assertEquals(1, config.packages.length);
        assertEquals("com.example", config.packages[0].path);
        assertNotNull(config.packages[0].exclude);
        assertEquals(0, config.packages[0].exclude.length);
    }
}

