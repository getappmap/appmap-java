package com.appland.appmap.test.util;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.util.ClassPoolExtension;

import javassist.CtClass;

@ExtendWith(ClassPoolExtension.class)
public class MethodBuilderTest {
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

    String actualOut = tapSystemOut(() -> myMethod.invoke(obj));
    assertEquals(myMethodMessage + System.getProperty("line.separator"), actualOut);
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

    String actualOut = tapSystemOut(() -> myMethod.invoke(obj, x, y));
    assertEquals(x + " " + y + System.getProperty("line.separator"), actualOut);
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