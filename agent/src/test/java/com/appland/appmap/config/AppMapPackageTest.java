package com.appland.appmap.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AppMapPackageTest {
  <T> T loadYaml(String[] yaml, Class<T> c) throws Exception {
    final String input = String.join("\n", yaml);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return (T) mapper.readValue(input, c);
  }

  //@formatter:off
  final static String[] LABEL_SPEC = {
    "---",
    "class: (Foo|Bar)", 
    "name: (foo|bar)",
    "labels: [foo]"
  };

  final static String[] METHOD_ONLY_SPEC = {
    "---",
    "class: (Foo|Bar)", 
    "name: (foo|bar)",
  };
  final static String[] BROKEN_REGEX_SPEC = {
    "---",
    "class: (Foo|Bar", 
    "name: (foo|bar)",
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

  @Nested
  class LabelConfigTests {
    private AppMapPackage.LabelConfig lc;
    @Nested
    class WithFullSpec {
      @BeforeEach
      public void load() throws Exception {
        lc = loadYaml(LABEL_SPEC, AppMapPackage.LabelConfig.class);
        assertEquals("foo", lc.getLabels()[0]);
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

    @Test
    void testMissingLabels() throws Exception {
      lc = loadYaml(METHOD_ONLY_SPEC, AppMapPackage.LabelConfig.class);
      assertNull(lc.labels);
    }

    @Test
    void testBrokenRegex() throws Exception {
      assertThrows(JsonProcessingException.class,
          () -> loadYaml(BROKEN_REGEX_SPEC, AppMapPackage.LabelConfig.class));
    }

  }

  @Nested
  class PackageTests {
    AppMapPackage appMapPackage;

    @Test
    public void testLoadConfig() throws Exception {
      appMapPackage = loadYaml(PACKAGE_CONFIG, AppMapPackage.class);
      assertEquals("com.example", appMapPackage.path);
      assertNotNull(appMapPackage.methods);
    }
  }
}