package com.appland.appmap.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PrefixTrieTest {

  @Nested
  class BasicOperations {
    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
      trie = new PrefixTrie();
    }

    @Test
    void testEmptyTrie() {
      assertFalse(trie.startsWith("anything"), "Empty trie should not match any string");
      assertFalse(trie.startsWith(""), "Empty trie should not match empty string");
    }

    @Test
    void testSingleInsertExactMatch() {
      trie.insert("foo");
      assertTrue(trie.startsWith("foo"), "Should match exact string");
    }

    @Test
    void testSingleInsertPrefixMatch() {
      trie.insert("foo");
      assertTrue(trie.startsWith("foobar"), "Should match when pattern is a prefix");
      assertTrue(trie.startsWith("foo.bar"), "Should match when pattern is a prefix");
    }

    @Test
    void testSingleInsertNoMatch() {
      trie.insert("foo");
      assertFalse(trie.startsWith("bar"), "Should not match unrelated string");
      assertFalse(trie.startsWith("fo"), "Should not match partial prefix");
      assertFalse(trie.startsWith("f"), "Should not match single character");
    }

    @Test
    void testEmptyStringInsert() {
      trie.insert("");
      assertTrue(trie.startsWith(""), "Should match empty string when empty string is inserted");
      assertTrue(trie.startsWith("anything"), "Empty pattern at root matches non-empty strings");
    }

    @Test
    void testNullHandling() {
      trie.insert(null);
      assertFalse(trie.startsWith(null), "Null should not match anything");

      trie.insert("foo");
      assertFalse(trie.startsWith(null), "Null should not match even when trie has entries");
    }
  }

  @Nested
  class MultiplePatterns {
    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
      trie = new PrefixTrie();
    }

    @Test
    void testMultipleDistinctPatterns() {
      trie.insert("foo");
      trie.insert("bar");
      trie.insert("baz");

      assertTrue(trie.startsWith("foobar"), "Should match first pattern");
      assertTrue(trie.startsWith("barbell"), "Should match second pattern");
      assertTrue(trie.startsWith("bazinga"), "Should match third pattern");
      assertFalse(trie.startsWith("qux"), "Should not match uninserted pattern");
    }

    @Test
    void testOverlappingPatterns() {
      trie.insert("foo");
      trie.insert("foobar");

      assertTrue(trie.startsWith("foo"), "Should match shorter pattern");
      assertTrue(trie.startsWith("foobar"), "Should match longer pattern");
      assertTrue(trie.startsWith("foobarbaz"), "Should match shortest prefix (foo)");
    }

    @Test
    void testPrefixOfPrefix() {
      trie.insert("a");
      trie.insert("ab");
      trie.insert("abc");

      assertTrue(trie.startsWith("a"), "Should match 'a'");
      assertTrue(trie.startsWith("ab"), "Should match 'ab'");
      assertTrue(trie.startsWith("abc"), "Should match 'abc'");
      assertTrue(trie.startsWith("abcd"), "Should match via 'a' prefix");
      assertFalse(trie.startsWith("b"), "Should not match 'b'");
    }
  }

  @Nested
  class PackageScenarios {
    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
      trie = new PrefixTrie();
    }

    @Test
    void testPackageExclusion() {
      trie.insert("internal.");

      assertTrue(trie.startsWith("internal.Foo"), "Should match class in excluded package");
      assertTrue(trie.startsWith("internal.sub.Bar"), "Should match class in excluded subpackage");
      assertFalse(trie.startsWith("internal"), "Should not match package name without separator");
      assertFalse(trie.startsWith("internals.Foo"), "Should not match similar package with separator");
    }

    @Test
    void testPackageBoundary() {
      trie.insert("test.");

      assertTrue(trie.startsWith("test.Foo"), "Should match class in test package");
      assertTrue(trie.startsWith("test.sub.Bar"), "Should match class in test subpackage");
      assertFalse(trie.startsWith("test"), "Should not match package name without separator");
      assertFalse(trie.startsWith("testing"), "Should not match similar package");
    }

    @Test
    void testClassExclusion() {
      trie.insert("util.Helper.");

      assertTrue(trie.startsWith("util.Helper.method"), "Should match method in excluded class");
      assertFalse(trie.startsWith("util.Helper"), "Should not match class name without separator");
      assertFalse(trie.startsWith("util.HelperUtils"), "Should not match similar class name");
      assertFalse(trie.startsWith("util"), "Should not match package alone");
    }

    @Test
    void testMethodExclusion() {
      trie.insert("Cache.clear");

      assertTrue(trie.startsWith("Cache.clear"), "Should match method exactly");
      assertTrue(trie.startsWith("Cache.clearAll"), "Will match since 'Cache.clear' is a prefix of 'Cache.clearAll'");
      assertFalse(trie.startsWith("Cache"), "Should not match class alone");
    }

    @Test
    void testMixedExclusions() {
      trie.insert("internal");       // whole package
      trie.insert("util.Helper");    // specific class
      trie.insert("Cache.clear");    // specific method
      trie.insert("test.");          // package with separator

      assertTrue(trie.startsWith("internal.Foo.bar"), "Should match package exclusion");
      assertTrue(trie.startsWith("util.Helper.method"), "Should match class exclusion");
      assertTrue(trie.startsWith("Cache.clear"), "Should match method exclusion");
      assertTrue(trie.startsWith("test.Foo"), "Should match package with separator");

      assertFalse(trie.startsWith("util.Other"), "Should not match other class in util");
      assertFalse(trie.startsWith("Cache.get"), "Should not match other method in Cache");
    }
  }

  @Nested
  class HierarchicalPatterns {
    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
      trie = new PrefixTrie();
    }

    @Test
    void testDeeplyNestedPackages() {
      trie.insert("com.example.internal");

      assertTrue(trie.startsWith("com.example.internal"), "Should match exact package");
      assertTrue(trie.startsWith("com.example.internal.Foo"), "Should match class in package");
      assertTrue(trie.startsWith("com.example.internal.sub.Bar"), "Should match class in subpackage");
      assertFalse(trie.startsWith("com.example"), "Should not match parent package");
      assertFalse(trie.startsWith("com.example.public"), "Should not match sibling package");
    }

    @Test
    void testMultipleLevelsOfExclusion() {
      trie.insert("com");
      trie.insert("com.example");
      trie.insert("com.example.foo");

      assertTrue(trie.startsWith("com.anything"), "Should match via 'com' prefix");
      assertTrue(trie.startsWith("com.example.anything"), "Should match via 'com' prefix");
      assertTrue(trie.startsWith("com.example.foo.Bar"), "Should match via 'com' prefix");
    }

    @Test
    void testFullyQualifiedNames() {
      trie.insert("com.example.MyClass.myMethod");

      assertTrue(trie.startsWith("com.example.MyClass.myMethod"), "Should match exact FQN");
      assertFalse(trie.startsWith("com.example.MyClass.otherMethod"), "Should not match different method");
      assertFalse(trie.startsWith("com.example.MyClass"), "Should not match just the class");
    }
  }

  @Nested
  class EdgeCases {
    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
      trie = new PrefixTrie();
    }

    @Test
    void testSingleCharacterPatterns() {
      trie.insert("a");

      assertTrue(trie.startsWith("a"), "Should match single character");
      assertTrue(trie.startsWith("abc"), "Should match when single char is prefix");
      assertFalse(trie.startsWith("b"), "Should not match different character");
    }

    @Test
    void testSpecialCharacters() {
      trie.insert("foo$bar");
      trie.insert("baz#qux");

      assertTrue(trie.startsWith("foo$bar"), "Should match pattern with $");
      assertTrue(trie.startsWith("foo$barbaz"), "Should match when $ pattern is prefix");
      assertTrue(trie.startsWith("baz#qux"), "Should match pattern with #");
      assertFalse(trie.startsWith("foo"), "Should not match partial before special char");
    }

    @Test
    void testDuplicateInsertions() {
      trie.insert("foo");
      trie.insert("foo");
      trie.insert("foo");

      assertTrue(trie.startsWith("foobar"), "Should still work after duplicate insertions");
    }

    @Test
    void testLongStrings() {
      String longPattern = "com.example.very.long.package.name.with.many.segments.MyClass.myMethod";
      trie.insert(longPattern);

      assertTrue(trie.startsWith(longPattern), "Should match long pattern exactly");
      assertTrue(trie.startsWith(longPattern + ".extra"), "Should match long pattern as prefix");
      assertFalse(trie.startsWith("com.example.very.long.package"), "Should not match partial");
    }

    @Test
    void testUnicodeCharacters() {
      trie.insert("café");
      trie.insert("日本語");

      assertTrue(trie.startsWith("café.method"), "Should match unicode pattern");
      assertTrue(trie.startsWith("日本語.クラス"), "Should match Japanese characters");
    }
  }

  @Nested
  class PrefixMatchingBehavior {
    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
      trie = new PrefixTrie();
    }

    @Test
    void testExactMatchIsPrefix() {
      trie.insert("exact");

      assertTrue(trie.startsWith("exact"), "Exact match should return true");
    }

    @Test
    void testLongerThanPattern() {
      trie.insert("short");

      assertTrue(trie.startsWith("short.longer.path"), "Longer string should match");
    }

    @Test
    void testShorterThanPattern() {
      trie.insert("verylongpattern");

      assertFalse(trie.startsWith("verylong"), "Shorter string should not match");
      assertFalse(trie.startsWith("very"), "Much shorter string should not match");
    }

    @Test
    void testFirstMatchWins() {
      trie.insert("foo");
      trie.insert("foobar");
      trie.insert("foobarbaz");

      // When checking "foobarbazqux", it should match "foo" first
      assertTrue(trie.startsWith("foobarbazqux"), "Should match shortest prefix");
    }

    @Test
    void testNoPartialPrefixMatch() {
      trie.insert("complete");

      assertFalse(trie.startsWith("comp"), "Should not match partial prefix");
      assertFalse(trie.startsWith("compl"), "Should not match partial prefix");
      assertFalse(trie.startsWith("complet"), "Should not match partial prefix");
      assertTrue(trie.startsWith("complete"), "Should match complete pattern");
      assertTrue(trie.startsWith("complete.more"), "Should match with additional text");
    }
  }

  @Nested
  class RealWorldScenarios {
    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
      trie = new PrefixTrie();
    }

    @Test
    void testCommonExclusionPatterns() {
      // Typical AppMap exclusion patterns
      trie.insert("internal");
      trie.insert("test");
      trie.insert("generated");
      trie.insert("impl.Helper");
      trie.insert("util.StringUtil.intern");

      // Should match
      assertTrue(trie.startsWith("internal.SecretClass.method"));
      assertTrue(trie.startsWith("test.MockService.setup"));
      assertTrue(trie.startsWith("generated.AutoValue_Foo"));
      assertTrue(trie.startsWith("impl.Helper.doSomething"));
      assertTrue(trie.startsWith("util.StringUtil.intern"));

      // Should not match
      assertFalse(trie.startsWith("impl.OtherClass"));
      assertFalse(trie.startsWith("util.StringUtil.format"));
      assertFalse(trie.startsWith("public.ApiClass"));
    }

    @Test
    void testJavaStandardLibraryExclusions() {
      trie.insert("java.");
      trie.insert("javax.");
      trie.insert("sun.");
      trie.insert("com.sun.");

      assertTrue(trie.startsWith("java.lang.String"));
      assertTrue(trie.startsWith("javax.servlet.HttpServlet"));
      assertTrue(trie.startsWith("sun.misc.Unsafe"));
      assertTrue(trie.startsWith("com.sun.management.GarbageCollectorMXBean"));

      assertFalse(trie.startsWith("javalin.Context"));
      assertFalse(trie.startsWith("com.example.Service"));
    }

    @Test
    void testFrameworkInternalExclusions() {
      trie.insert("org.springframework.cglib");
      trie.insert("org.hibernate.internal");
      trie.insert("net.bytebuddy");

      assertTrue(trie.startsWith("org.springframework.cglib.Enhancer"));
      assertTrue(trie.startsWith("org.hibernate.internal.SessionImpl"));
      assertTrue(trie.startsWith("net.bytebuddy.ByteBuddy"));

      assertFalse(trie.startsWith("org.springframework.web.Controller"));
      assertFalse(trie.startsWith("org.hibernate.Session"));
    }
  }
}
