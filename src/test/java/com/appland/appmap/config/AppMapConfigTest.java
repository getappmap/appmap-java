package com.appland.appmap.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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

    /**
     * Test of nested shallow package configuration
     */
    @Test
    public void testNestedShallowConfig() {
        AppMapConfig cfg = AppMapConfig.load(new File("build/resources/test/appmap_nested_shallow_test.yml"));
        PackageConfig pkgCfg;

        pkgCfg = cfg.getPackageConfig("a");
        assertNotNull(pkgCfg);
        assertEquals(pkgCfg.includedPackageName, "a");
        assertTrue(!pkgCfg.shallow);

        pkgCfg = cfg.getPackageConfig("b");
        assertNotNull(pkgCfg);
        assertEquals(pkgCfg.includedPackageName, "b");
        assertTrue(pkgCfg.shallow);

        pkgCfg = cfg.getPackageConfig("b.d");
        assertNotNull(pkgCfg);
        assertEquals(pkgCfg.includedPackageName, "b");
        assertTrue(pkgCfg.shallow);

        pkgCfg = cfg.getPackageConfig("b.b");
        assertNotNull(pkgCfg);
        assertEquals(pkgCfg.includedPackageName, "b.b");
        assertTrue(pkgCfg.shallow);

        pkgCfg = cfg.getPackageConfig("b.c.a");
        assertNotNull(pkgCfg);
        assertEquals(pkgCfg.includedPackageName, "b.c");
        assertTrue(!pkgCfg.shallow);

        pkgCfg = cfg.getPackageConfig("b.c.b");
        assertNotNull(pkgCfg);
        assertEquals(pkgCfg.includedPackageName, "b.c.b");
        assertTrue(pkgCfg.shallow);

        //package x is not included in appmap.yml
        pkgCfg = cfg.getPackageConfig("x");
        assertTrue(pkgCfg == null);

        //Memoization test. We want the same object reference returned.
        assertTrue(cfg.getPackageConfig("a") == cfg.getPackageConfig("a"));

    }
}


