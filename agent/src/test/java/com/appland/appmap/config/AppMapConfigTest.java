package com.appland.appmap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
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

    /**
     * The test should pass since since issue 53 fix, the AppMapConfig logs to StdErr even if debug property is not
     * set.
     */
    @Test
    public void load() {
        // Trying to load appmap.yml in /tmp shouldn't work.
        AppMapConfig.load(Paths.get(System.getProperty("java.io.tmpdir"), "appmap.yml").toFile());
        assertNotNull(errContent.toString());
        assertTrue(errContent.toString().contains("file not found"));
    }

    // If a non-existent config file in a subdirectory is specified, the
    // config file in the current directory should be used.
    @Test
    public void loadParent() {
        File f = Paths.get("test", "appmap.yml").toFile();
        assertFalse(f.exists());
        AppMapConfig.load(f);
        File expected = Paths.get("appmap.yml").toAbsolutePath().toFile();
        assertTrue(expected.exists()); // sanity check
        assertEquals(expected, AppMapConfig.get().configFile);
    }

}


