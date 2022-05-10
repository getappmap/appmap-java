package com.appland.appmap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

class Tests {
  static class Fixtures {
    //@formatter:off
    final static String[] LABEL_SPEC = {
      "---",
      "class: (Foo|Bar)", 
      "name: (foo|bar)",
      "labels: [foo]"
    };
 
    final static String[] PACKAGE_CONFIG = {
      "---",
      "path: com.example",
      "exclude: [com.example.Spam.ham]",
      "methods:",
      "- class: (Foo|Bar)",
      "  name: (foo|bar)",
      "  labels: [foo]"
    };
    //@formatter:on
  }

  public static class LabelConfigTests {
    private AppMapPackage.LabelConfig lc;

    @Before
    public void load() {
      final String input = String.join("\n", Fixtures.LABEL_SPEC);

      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        lc = mapper.readValue(input, AppMapPackage.LabelConfig.class);
        assertEquals("foo", lc.getLabels()[0]);
      } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }

    @Test
    public void testMatches() {
      assertTrue(lc.matches("Foo", "bar"));
    }

    @Test
    public void testMatchesWholeClass() {
      assertFalse(lc.matches("Foo$Foo1", "bar"));
    }

    @Test
    public void testMatchesWholeName() {
      assertFalse(lc.matches("Foo", "bar!"));
    }
  }

  public static class PackageTests {
    @Test
    public void testLoadConfig() {
      final String input = String.join("\n", Fixtures.PACKAGE_CONFIG);
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        final AppMapPackage appMapPackage = mapper.readValue(input, AppMapPackage.class);
        assertEquals("com.example", appMapPackage.path);
        assertNotNull(appMapPackage.methods);

      } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
  }
}


@RunWith(Suite.class)
@Suite.SuiteClasses({Tests.LabelConfigTests.class, Tests.PackageTests.class})

public class AppMapPackageTest {
}

