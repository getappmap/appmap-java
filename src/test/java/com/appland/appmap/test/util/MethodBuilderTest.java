package com.appland.appmap.test.util;

import com.appland.appmap.output.v1.Parameters;
import javassist.CtClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MethodBuilderTest {
  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private static final String myMethodMessage = "calling myMethod";

  @Test
  public void testBuild() throws Exception {
    Class<?> testClass = new ClassBuilder("MethodBuilderTest.testBuild")
        .beginMethod("myMethod")
          .addModifer(Modifier.PUBLIC)
          .setBody("{ System.out.println(\"" + myMethodMessage + "\"); }")
        .endMethod()
        .build()
        .asClass();

    Object obj = testClass.getDeclaredConstructor().newInstance();
    Method myMethod = testClass.getMethod("myMethod");

    myMethod.invoke(obj);
    assertEquals(myMethodMessage + "\n", systemOutRule.getLog());
  }

  @Test
  public void testMultiParameter() throws Exception {
    NewClass testClass = new ClassBuilder("MethodBuilderTest.myMethod")
        .beginMethod("myMethod")
          .addModifer(Modifier.PUBLIC)
          .addParameter(CtClass.intType, "x")
          .addParameter(CtClass.doubleType, "y")
          .setBody("{ System.out.println(x + \" \" + y); }")
        .endMethod()
        .build();

    Object obj = testClass.asClass().getDeclaredConstructor().newInstance();
    Method myMethod = testClass.asClass().getMethod("myMethod", int.class, double.class);
    int x = 100;
    double y = 12.0;

    myMethod.invoke(obj, x, y);
    assertEquals(x + " " + y + "\n", systemOutRule.getLog());
    assertEquals(2, myMethod.getParameterCount());

    assertArrayEquals(new Class<?>[]{ int.class, double.class }, myMethod.getParameterTypes());

    // HACK
    // I'm not sure why I can't get parameter names through reflection of the loaded class.
    // Uncommenting the following code will break the test
    // - db
    //
    // Parameter xParam = myMethod.getParameters()[0];
    // assertTrue(xParam.isNamePresent());
    // assertEquals("x", xParam.getName());

    // The class must not be frozen in order to get the parameter names
    testClass.asCtClass().defrost();

    Parameters p = new Parameters(testClass.asCtClass().getDeclaredMethods()[0]);
    assertEquals("x", p.get(0).name);
    assertEquals("y", p.get(1).name);
  }
}