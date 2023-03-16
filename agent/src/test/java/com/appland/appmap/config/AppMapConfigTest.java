package com.appland.appmap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AppMapConfigTest {

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setErr(originalErr);
    }

    @Test
    public void loadBadDirectory() {
        // Trying to load appmap.yml in non-existent directory shouldn't work.
        Path badDir = Paths.get("/no-such-dir");
        assertFalse(Files.exists(badDir));
        AppMapConfig.load(badDir.resolve("appmap.yml").toFile(), false);
        assertNotNull(errContent.toString());
        assertTrue(errContent.toString().contains("file not found"));
    }

    @Test
    public void createDefault() throws IOException {
        // Just verify that the file gets created when it should. The contents
        // get verified elsewhere.
        Path configFile = Paths.get(System.getProperty("java.io.tmpdir"), "appmap.yml");
        Files.deleteIfExists(configFile);
        AppMapConfig.load(configFile.toFile(), false);
        assertTrue(Files.exists(configFile));
    }

    @Test
    public void preservesExisting() throws IOException {
        Path tmpConfigFile = Files.createTempFile("appmap-config.", ".yml");
        String expectedName = "not-a-real-app";
        // This isn't the name in the default config
        Files.write(tmpConfigFile, String.format("name: %s\n", expectedName).getBytes());
        AppMapConfig actual = AppMapConfig.load(tmpConfigFile.toFile(), false);
        assertEquals(expectedName, actual.name);
    }

    @Test
    public void requiresExisting() throws IOException {
        Path configFile = Paths.get(System.getProperty("java.io.tmpdir"), "appmap.yml");
        Files.deleteIfExists(configFile);

        AppMapConfig.load(configFile.toFile(), true);
        assertNotNull(errContent.toString());
        assertTrue(errContent.toString().contains("file not found"));
    }

    // If a non-existent config file in a subdirectory is specified, the
    // config file in the current directory should be used.
    @Test
    public void loadParent() {
        File f = Paths.get("test", "appmap.yml").toFile();
        assertFalse(f.exists());
        AppMapConfig.load(f, false);
        File expected = Paths.get("appmap.yml").toAbsolutePath().toFile();
        assertTrue(expected.exists()); // sanity check
        assertEquals(expected, AppMapConfig.get().configFile);
    }

}


