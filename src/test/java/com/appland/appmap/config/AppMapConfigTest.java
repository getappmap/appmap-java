package com.appland.appmap.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        AppMapConfig.load(new File("agent_conf.yml"));
        assertNotNull(errContent.toString());
        assertTrue(errContent.toString().contains("file not found"));
    }
}


