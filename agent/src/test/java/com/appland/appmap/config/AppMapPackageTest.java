package com.appland.appmap.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appland.appmap.util.FullyQualifiedName;
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
    AppMapPackage.LabelConfig lc;
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
      assertNull(lc.getLabels());
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

  @Nested
  class ExcludeModeTests {
    @Nested
    class BasicMatching {
      AppMapPackage pkg;

      @BeforeEach
      public void setup() throws Exception {
        String[] yaml = {
            "---",
            "path: com.example"
        };
        pkg = loadYaml(yaml, AppMapPackage.class);
      }

      @Test
      public void testMatchesMethodInPackage() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Foo", false, "bar");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should match methods in the configured package");
        // In exclude mode, a new LabelConfig() is returned which has an empty array for
        // labels
        assertNotNull(result.getLabels(), "Labels should be non-null");
        assertEquals(0, result.getLabels().length, "Should have no labels in exclude mode");
      }

      @Test
      public void testMatchesMethodInSubpackage() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example.sub", "Foo", false, "bar");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should match methods in subpackages");
      }

      @Test
      public void testDoesNotMatchMethodOutsidePackage() {
        FullyQualifiedName fqn = new FullyQualifiedName("org.other", "Foo", false, "bar");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should not match methods outside the package");
      }

      @Test
      public void testDoesNotMatchPartialPackageName() {
        // Package is "com.example", should not match "com.examples"
        FullyQualifiedName fqn = new FullyQualifiedName("com.examples", "Foo", false, "bar");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should not match partial package names");
      }
    }

    @Nested
    class WithExclusions {
      AppMapPackage pkg;

      @BeforeEach
      public void setup() throws Exception {
        String[] yaml = {
            "---",
            "path: com.example",
            "exclude: [Internal, com.example.Private, Secret.sensitiveMethod]"
        };
        pkg = loadYaml(yaml, AppMapPackage.class);
      }

      @Test
      public void testExcludesRelativeClassName() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Internal", false, "foo");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should exclude relative class name");
      }

      @Test
      public void testExcludesAbsoluteClassName() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Private", false, "foo");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should exclude absolute class name");
      }

      @Test
      public void testExcludesSpecificMethod() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Secret", false, "sensitiveMethod");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should exclude specific method");
      }

      @Test
      public void testDoesNotExcludeOtherMethodsInExcludedClass() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Secret", false, "publicMethod");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should not exclude other methods in partially excluded class");
      }

      @Test
      public void testIncludesNonExcludedClass() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Public", false, "foo");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should include non-excluded classes");
      }

      @Test
      public void testExcludesSubclassesOfExcludedClass() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Internal$Inner", false, "foo");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should exclude nested classes of excluded class");
      }
    }

    @Nested
    class WithHashSeparator {
      AppMapPackage pkg;

      @BeforeEach
      public void setup() throws Exception {
        String[] yaml = {
            "---",
            "path: com.example",
            "exclude: [Foo#bar, Internal#secretMethod]"
        };
        pkg = loadYaml(yaml, AppMapPackage.class);
      }

      @Test
      public void testConvertsHashToDot() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Foo", false, "bar");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should convert # to . for backward compatibility");
      }

      @Test
      public void testDoesNotExcludeOtherMethods() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Foo", false, "baz");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should not exclude methods not specified");
      }
    }
  }

  @Nested
  class MethodsModeTests {
    @Nested
    class BasicPatternMatching {
      AppMapPackage pkg;

      @BeforeEach
      public void setup() throws Exception {
        String[] yaml = {
            "---",
            "path: com.example",
            "methods:",
            "- class: Controller",
            "  name: handle.*",
            "  labels: [controller]",
            "- class: Service",
            "  name: process",
            "  labels: [service, business-logic]"
        };
        pkg = loadYaml(yaml, AppMapPackage.class);
      }

      @Test
      public void testMatchesSimpleClassName() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Controller", false, "handleRequest");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should match simple class name");
        assertArrayEquals(new String[] { "controller" }, result.getLabels());
      }

      @Test
      public void testMatchesMethodPattern() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Controller", false, "handleResponse");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should match method name pattern");
        assertArrayEquals(new String[] { "controller" }, result.getLabels());
      }

      @Test
      public void testDoesNotMatchNonMatchingMethod() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Controller", false, "initialize");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should not match non-matching method name");
      }

      @Test
      public void testMatchesExactMethodName() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Service", false, "process");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should match exact method name");
        assertArrayEquals(new String[] { "service", "business-logic" }, result.getLabels());
      }

      @Test
      public void testDoesNotMatchPartialMethodName() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Service", false, "processData");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should not match partial method name (no implicit wildcards)");
      }

      @Test
      public void testDoesNotMatchDifferentPackage() {
        FullyQualifiedName fqn = new FullyQualifiedName("org.other", "Controller", false, "handleRequest");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Should not match methods in different package");
      }

      @Test
      public void testDoesNotMatchSubpackage() {
        // In methods mode, package must match exactly (not subpackages)
        FullyQualifiedName fqn = new FullyQualifiedName("com.example.sub", "Controller", false, "handleRequest");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNull(result, "Methods mode should not match subpackages");
      }
    }

    @Nested
    class FullyQualifiedClassNames {
      AppMapPackage pkg;

      @BeforeEach
      public void setup() throws Exception {
        String[] yaml = {
            "---",
            "path: com.example",
            "methods:",
            "- class: com.example.web.Controller",
            "  name: handle.*",
            "  labels: [web-controller]"
        };
        pkg = loadYaml(yaml, AppMapPackage.class);
      }

      @Test
      public void testMatchesFullyQualifiedClassName() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "web.Controller", false, "handleGet");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should match fully qualified class name pattern");
        assertArrayEquals(new String[] { "web-controller" }, result.getLabels());
      }
    }

    @Nested
    class RegexPatterns {
      AppMapPackage pkg;

      @BeforeEach
      public void setup() throws Exception {
        String[] yaml = {
            "---",
            "path: com.example",
            "methods:",
            "- class: (Controller|Handler)",
            "  name: (get|set).*",
            "  labels: [accessor]",
            "- class: .*Service",
            "  name: execute",
            "  labels: [service-executor]"
        };
        pkg = loadYaml(yaml, AppMapPackage.class);
      }

      @Test
      public void testMatchesClassAlternation() {
        FullyQualifiedName fqn1 = new FullyQualifiedName("com.example", "Controller", false, "getData");
        FullyQualifiedName fqn2 = new FullyQualifiedName("com.example", "Handler", false, "setData");

        AppMapPackage.LabelConfig result1 = pkg.find(fqn1);
        AppMapPackage.LabelConfig result2 = pkg.find(fqn2);

        assertNotNull(result1, "Should match first class alternative");
        assertNotNull(result2, "Should match second class alternative");
        assertArrayEquals(new String[] { "accessor" }, result1.getLabels());
        assertArrayEquals(new String[] { "accessor" }, result2.getLabels());
      }

      @Test
      public void testMatchesClassWildcard() {
        FullyQualifiedName fqn1 = new FullyQualifiedName("com.example", "UserService", false, "execute");
        FullyQualifiedName fqn2 = new FullyQualifiedName("com.example", "OrderService", false, "execute");

        AppMapPackage.LabelConfig result1 = pkg.find(fqn1);
        AppMapPackage.LabelConfig result2 = pkg.find(fqn2);

        assertNotNull(result1, "Should match first service");
        assertNotNull(result2, "Should match second service");
        assertArrayEquals(new String[] { "service-executor" }, result1.getLabels());
        assertArrayEquals(new String[] { "service-executor" }, result2.getLabels());
      }
    }

    @Nested
    class IgnoresExcludeField {
      AppMapPackage pkg;

      @BeforeEach
      public void setup() throws Exception {
        String[] yaml = {
            "---",
            "path: com.example",
            "exclude: [Controller]", // This should be ignored
            "methods:",
            "- class: Controller",
            "  name: handleRequest",
            "  labels: [controller]"
        };
        pkg = loadYaml(yaml, AppMapPackage.class);
      }

      @Test
      public void testIgnoresExcludeWhenMethodsIsSet() {
        FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Controller", false, "handleRequest");
        AppMapPackage.LabelConfig result = pkg.find(fqn);
        assertNotNull(result, "Should ignore exclude field when methods is set");
        assertArrayEquals(new String[] { "controller" }, result.getLabels());
      }
    }
  }

  @Nested
  class EdgeCases {
    @Test
    public void testNullPath() throws Exception {
      String[] yaml = {
          "---",
          "path: null"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);
      FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Foo", false, "bar");
      assertNull(pkg.find(fqn), "Should handle null path gracefully");
    }

    @Test
    public void testNullCanonicalName() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);
      assertNull(pkg.find(null), "Should handle null canonical name gracefully");
    }

    @Test
    public void testEmptyExclude() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example",
          "exclude: []"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);
      assertEquals(0, pkg.exclude.length, "Should handle empty exclude array");
    }

    @Test
    public void testNullExclude() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example",
          "exclude:"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);
      assertNotNull(pkg.exclude, "Should initialize exclude to empty array");
      assertEquals(0, pkg.exclude.length, "Should handle null exclude array");
    }

    @Test
    public void testNoExclude() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);
      assertNotNull(pkg.exclude, "Should initialize exclude to empty array");
      assertEquals(0, pkg.exclude.length);
    }

    @Test
    public void testShallowDefault() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);
      assertFalse(pkg.shallow, "shallow should default to false");
    }

    @Test
    public void testShallowTrue() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example",
          "shallow: true"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);
      assertTrue(pkg.shallow, "shallow should be set to true");
    }
  }

  @Nested
  class EnhancedLabelConfigTests {
    @Test
    public void testEmptyLabelConfig() {
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig();
      // Empty constructor uses field initialization, which is an empty array
      assertNotNull(lc.getLabels(), "Empty LabelConfig should have non-null labels");
      assertEquals(0, lc.getLabels().length, "Empty LabelConfig should have empty labels array");
    }

    @Test
    public void testLabelConfigWithLabels() throws Exception {
      String[] yaml = {
          "---",
          "class: Foo",
          "name: bar",
          "labels: [test, example]"
      };
      AppMapPackage.LabelConfig lc = loadYaml(yaml, AppMapPackage.LabelConfig.class);
      assertNotNull(lc.getLabels());
      assertEquals(2, lc.getLabels().length);
      assertEquals("test", lc.getLabels()[0]);
      assertEquals("example", lc.getLabels()[1]);
    }

    @Test
    public void testLabelConfigMatchesSimpleClass() {
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig("Controller", "handle.*", new String[] { "web" });
      FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Controller", false, "handleGet");
      assertTrue(lc.matches(fqn), "Should match simple class name");
    }

    @Test
    public void testLabelConfigMatchesFullyQualifiedClass() {
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig("com.example.Controller", "handle.*",
          new String[] { "web" });
      FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Controller", false, "handleGet");
      assertTrue(lc.matches(fqn), "Should match fully qualified class name");
    }

    @Test
    public void testLabelConfigDoesNotMatchWrongClass() {
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig("Controller", "handle.*", new String[] { "web" });
      FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Service", false, "handleGet");
      assertFalse(lc.matches(fqn), "Should not match wrong class");
    }

    @Test
    public void testLabelConfigDoesNotMatchWrongMethod() {
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig("Controller", "handle.*", new String[] { "web" });
      FullyQualifiedName fqn = new FullyQualifiedName("com.example", "Controller", false, "process");
      assertFalse(lc.matches(fqn), "Should not match wrong method");
    }

    @Test
    public void testLabelConfigMatchesExactPattern() {
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig("Foo", "bar", new String[] { "test" });
      assertTrue(lc.matches("Foo", "bar"), "Should match exact patterns");
    }

    @Test
    public void testLabelConfigDoesNotMatchPartialClass() {
      // Pattern "Foo" should not match "Foo1" due to anchoring
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig("Foo", "bar", new String[] { "test" });
      assertFalse(lc.matches("Foo1", "bar"), "Should not match partial class name");
    }

    @Test
    public void testLabelConfigDoesNotMatchPartialMethod() {
      // Pattern "bar" should not match "bar!" due to anchoring
      AppMapPackage.LabelConfig lc = new AppMapPackage.LabelConfig("Foo", "bar", new String[] { "test" });
      assertFalse(lc.matches("Foo", "bar!"), "Should not match partial method name");
    }
  }

  @Nested
  class ExcludesMethodTests {
    @Test
    public void testExcludesFullyQualifiedName() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example",
          "exclude: [Internal, Private.secret]"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);

      FullyQualifiedName fqn1 = new FullyQualifiedName("com.example", "Internal", false, "foo");
      FullyQualifiedName fqn2 = new FullyQualifiedName("com.example", "Private", false, "secret");
      FullyQualifiedName fqn3 = new FullyQualifiedName("com.example", "Public", false, "method");

      assertTrue(pkg.excludes(fqn1), "Should exclude Internal class");
      assertTrue(pkg.excludes(fqn2), "Should exclude Private.secret method");
      assertFalse(pkg.excludes(fqn3), "Should not exclude Public class");
    }
  }

  @Nested
  class ComplexScenarios {
    @Test
    public void testMultipleMethodConfigs() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example.api",
          "methods:",
          "- class: .*Controller",
          "  name: handle.*",
          "  labels: [web, controller]",
          "- class: .*Service",
          "  name: execute.*",
          "  labels: [service]",
          "- class: Repository",
          "  name: (find|save|delete).*",
          "  labels: [data-access, repository]"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);

      FullyQualifiedName controller = new FullyQualifiedName("com.example.api", "UserController", false, "handleGet");
      FullyQualifiedName service = new FullyQualifiedName("com.example.api", "UserService", false, "executeQuery");
      FullyQualifiedName repo = new FullyQualifiedName("com.example.api", "Repository", false, "findById");

      AppMapPackage.LabelConfig result1 = pkg.find(controller);
      AppMapPackage.LabelConfig result2 = pkg.find(service);
      AppMapPackage.LabelConfig result3 = pkg.find(repo);

      assertNotNull(result1);
      assertArrayEquals(new String[] { "web", "controller" }, result1.getLabels());

      assertNotNull(result2);
      assertArrayEquals(new String[] { "service" }, result2.getLabels());

      assertNotNull(result3);
      assertArrayEquals(new String[] { "data-access", "repository" }, result3.getLabels());
    }

    @Test
    public void testComplexExclusionPatterns() throws Exception {
      String[] yaml = {
          "---",
          "path: com.example",
          "exclude:",
          "  - internal",
          "  - util.Helper",
          "  - com.example.test.Mock",
          "  - Secret.getPassword",
          "  - Cache.clear"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);

      FullyQualifiedName internal = new FullyQualifiedName("com.example.internal", "Foo", false, "bar");
      FullyQualifiedName helper = new FullyQualifiedName("com.example.util", "Helper", false, "help");
      FullyQualifiedName mock = new FullyQualifiedName("com.example.test", "Mock", false, "setup");
      FullyQualifiedName secretGet = new FullyQualifiedName("com.example", "Secret", false, "getPassword");
      FullyQualifiedName secretSet = new FullyQualifiedName("com.example", "Secret", false, "setPassword");
      FullyQualifiedName cacheClear = new FullyQualifiedName("com.example", "Cache", false, "clear");
      FullyQualifiedName cacheGet = new FullyQualifiedName("com.example", "Cache", false, "get");

      assertNull(pkg.find(internal), "Should exclude internal package");
      assertNull(pkg.find(helper), "Should exclude util.Helper");
      assertNull(pkg.find(mock), "Should exclude test.Mock");
      assertNull(pkg.find(secretGet), "Should exclude Secret.getPassword");
      assertNotNull(pkg.find(secretSet), "Should not exclude Secret.setPassword");
      assertNull(pkg.find(cacheClear), "Should exclude Cache.clear");
      assertNotNull(pkg.find(cacheGet), "Should not exclude Cache.get");
    }

    @Test
    public void testUnnamedPackage() throws Exception {
      String[] yaml = {
          "---",
          "path: HelloWorld"
      };
      AppMapPackage pkg = loadYaml(yaml, AppMapPackage.class);

      // Test a method in the unnamed package (empty package name)
      FullyQualifiedName method = new FullyQualifiedName("", "HelloWorld", false, "getGreetingWithPunctuation");

      AppMapPackage.LabelConfig result = pkg.find(method);
      assertNotNull(result, "Should find method in unnamed package when path specifies the class name");

      // Test that other classes in the unnamed package are not matched
      FullyQualifiedName otherClass = new FullyQualifiedName("", "OtherClass", false, "someMethod");
      assertNull(pkg.find(otherClass), "Should not match other classes in the unnamed package");
    }
  }
}