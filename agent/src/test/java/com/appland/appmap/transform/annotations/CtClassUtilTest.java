package com.appland.appmap.transform.annotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.appland.appmap.util.AppMapClassPool;
import com.appland.appmap.util.ClassPoolExtension;

import javassist.CtClass;
import javassist.NotFoundException;

@ExtendWith(ClassPoolExtension.class)
public class CtClassUtilTest {
  @Test
  public void testChildOfSameClass() throws NotFoundException {
    CtClass candidateChildClass = AppMapClassPool.get().get("java.lang.Object");
    assertTrue(CtClassUtil.isChildOf(candidateChildClass, "java.lang.Object"));
  }

  @Test
  public void testValidChildOfSuperClass() throws NotFoundException {
    CtClass candidateChildClass = AppMapClassPool.get().get("java.lang.Integer");
    assertTrue(CtClassUtil.isChildOf(candidateChildClass, "java.lang.Object"));
  }

  @Test
  public void testInvalidChildOfSuperClass() throws NotFoundException {
    CtClass candidateChildClass = AppMapClassPool.get().get("java.lang.Object");
    assertFalse(CtClassUtil.isChildOf(candidateChildClass, "java.lang.Integer"));
  }

  @Test
  public void testValidChildOfImplementation() throws NotFoundException {
    CtClass candidateChildClass = AppMapClassPool.get().get("java.lang.Throwable");
    assertTrue(CtClassUtil.isChildOf(candidateChildClass, "java.io.Serializable"));
  }
}
