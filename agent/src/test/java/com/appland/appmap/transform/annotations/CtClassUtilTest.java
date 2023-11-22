package com.appland.appmap.transform.annotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class CtClassUtilTest {
  @Test
  public void testChildOfSameClass() throws NotFoundException {
    CtClass candidateChildClass = ClassPool.getDefault().get("java.lang.Object");
    assertTrue(CtClassUtil.isChildOf(candidateChildClass, "java.lang.Object"));
  }

  @Test
  public void testValidChildOfSuperClass() throws NotFoundException {
    CtClass candidateChildClass = ClassPool.getDefault().get("java.lang.Integer");
    assertTrue(CtClassUtil.isChildOf(candidateChildClass, "java.lang.Object"));
  }

  @Test
  public void testInvalidChildOfSuperClass() throws NotFoundException {
    CtClass candidateChildClass = ClassPool.getDefault().get("java.lang.Object");
    assertFalse(CtClassUtil.isChildOf(candidateChildClass, "java.lang.Integer"));
  }

  @Test
  public void testValidChildOfImplementation() throws NotFoundException {
    CtClass candidateChildClass = ClassPool.getDefault().get("java.lang.Throwable");
    assertTrue(CtClassUtil.isChildOf(candidateChildClass, "java.io.Serializable"));
  }
}
