package com.appland.appmap.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.appland.appmap.test.util.ParameterBuilder;

import org.junit.Before;
import org.junit.Test;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class ParameterBuilderTest {
  private String paramId = "myParam";
  private CtClass paramType;

  @Before
  public void before() throws Exception {
    paramId = "myParam";
    paramType = ClassPool.getDefault().get("java.lang.String");
  }

  @Test
  public void testInvalidTypeByName() throws Exception {
    final ClassBuilder classBuilder = new ClassBuilder("ParameterBuilderTest.testBuild");
    final MethodBuilder methodBuilder = new MethodBuilder(classBuilder);
    final ParameterBuilder parameterBuilder = new ParameterBuilder(methodBuilder);

    try {
      parameterBuilder.setType("this is a nonsense string");
    } catch (NotFoundException e) {
      return;
    }

    fail();
  }

  @Test
  public void testTypeByName() throws Exception {
    final ClassBuilder classBuilder = new ClassBuilder("ParameterBuilderTest.testBuild");
    final MethodBuilder methodBuilder = new MethodBuilder(classBuilder);
    final ParameterBuilder parameterBuilder = new ParameterBuilder(methodBuilder);

    parameterBuilder
        .setId(paramId)
        .setType(paramType.getName());

    assertEquals(paramId, parameterBuilder.getId());
    assertEquals(paramType, parameterBuilder.getType());
  }

  void testType() throws Exception {
    final ClassBuilder classBuilder = new ClassBuilder("ParameterBuilderTest.testBuild");
    final MethodBuilder methodBuilder = new MethodBuilder(classBuilder);
    final ParameterBuilder parameterBuilder = new ParameterBuilder(methodBuilder);

    parameterBuilder
        .setId(paramId)
        .setType(paramType);

    assertEquals(paramId, parameterBuilder.getId());
    assertEquals(paramType, parameterBuilder.getType());
  }
}