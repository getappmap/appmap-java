package com.appland.appmap.test.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.appland.appmap.util.ClassPoolExtension;

@ExtendWith(ClassPoolExtension.class)
public class ClassBuilderTest {
  @Test
  public void testBuild() throws Exception {
    Class<?> testClass = new ClassBuilder("ClassBuilderTest.testBuild").build().asClass();
    Object obj = testClass.getDeclaredConstructor().newInstance();

    assertTrue(testClass.isInstance(obj));
  }
} 