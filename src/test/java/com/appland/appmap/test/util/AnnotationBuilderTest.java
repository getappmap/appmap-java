package com.appland.appmap.test.util;

import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

public class AnnotationBuilderTest {
  @Test
  public void testSingleAnnotation() throws Exception {
    Class<?> newClass = new ClassBuilder("AnnotationBuilderTest.testBuild")
        .beginMethod("myMethod")
          .addModifer(Modifier.PUBLIC)
          .beginAnnotation(HookClass.class.getName())
            .setMember("value", "java.sql.Connection")
            .setMember("method", "prepareSQL")
          .endAnnotation()
        .endMethod()
        .build()
        .asClass();

    Method myMethod = newClass.getMethod("myMethod");
    HookClass annotation = (HookClass) myMethod.getAnnotation(HookClass.class);

    assertNotNull(annotation);
    assertEquals("java.sql.Connection", annotation.value());
    assertEquals("prepareSQL", annotation.method());
  }

  @Test
  public void testManyAnnotations() throws Exception {
    Class<?> newClass = new ClassBuilder("AnnotationBuilderTest.testBuild2")
        .beginMethod("myMethod")
          .addModifer(Modifier.PUBLIC)
          .beginAnnotation(HookClass.class.getName())
            .setMember("value", "java.sql.Connection")
            .setMember("method", "prepareSQL")
          .endAnnotation()
          .addAnnotation(ExcludeReceiver.class.getName())
        .endMethod()
        .build()
        .asClass();

    Method myMethod = newClass.getMethod("myMethod");
    HookClass hookAnno = (HookClass) myMethod.getAnnotation(HookClass.class);
    ExcludeReceiver excludeAnno = (ExcludeReceiver) myMethod.getAnnotation(ExcludeReceiver.class);

    assertNotNull(hookAnno);
    assertNotNull(excludeAnno);
    assertEquals("java.sql.Connection", hookAnno.value());
    assertEquals("prepareSQL", hookAnno.method());
    assertTrue(excludeAnno.value());
  }
}