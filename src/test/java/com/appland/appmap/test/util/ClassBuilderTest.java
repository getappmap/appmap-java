package com.appland.appmap.test.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import static org.junit.Assert.assertTrue;

public class ClassBuilderTest {
  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Test
  public void testBuild() throws Exception {
    Class<?> testClass = new ClassBuilder("ClassBuilderTest.testBuild").build().asClass();
    Object obj = testClass.getDeclaredConstructor().newInstance();

    assertTrue(testClass.isInstance(obj));
  }
} 