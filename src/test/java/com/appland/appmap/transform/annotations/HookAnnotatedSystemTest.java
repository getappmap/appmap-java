package com.appland.appmap.transform.annotations;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.test.util.ClassBuilder;

import org.junit.Before;
import org.junit.Test;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;

public class HookAnnotatedSystemTest {
  private final static String TargetClassName = "HookAnnotatedSystemTest.TargetClass";
  private final static String HookClassName = "HookAnnotatedSystemTest.HookClass";
  private final static Integer UNUSED_PARAMETER = -1;
  private CtClass targetClass;

  @Before
  public void initializeTestClasses() throws Exception {
    targetClass = new ClassBuilder(TargetClassName)
        .beginMethod()
          .setName("methodNoArgs")
          .addAnnotation(Test.class.getName())
        .endMethod()
        .beginMethod()
          .setName("methodSingleArg")
          .addParameter(CtClass.intType, "x")
          .addAnnotation(Test.class.getName())
        .endMethod()
        .beginMethod()
          .setName("methodManyArgs")
          .addParameter(CtClass.intType, "x")
          .addParameter(CtClass.intType, "y")
          .addAnnotation(Test.class.getName())
        .endMethod()
        .ctClass();
  }

  @Test
  public void testValidate() throws Exception {
    List<Hook> hooks = new ArrayList<Hook>();
    List<HookBinding> bindings = new ArrayList<HookBinding>();
    CtClass hookClass = new ClassBuilder(HookClassName)
        .beginMethod()
          .setName("methodNoArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassName, "receiver")
          .beginAnnotation(HookAnnotated.class.getName())
            .setMember("value", Test.class.getName())
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodSingleArg")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassName, "receiver")
          .addParameter(CtClass.intType, "x")
          .beginAnnotation(HookAnnotated.class.getName())
            .setMember("value", Test.class.getName())
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodManyArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassName, "receiver")
          .addParameter(CtClass.intType, "x")
          .addParameter(CtClass.intType, "y")
          .beginAnnotation(HookAnnotated.class.getName())
            .setMember("value", Test.class.getName())
          .endAnnotation()
        .endMethod()
        .ctClass();

    for (CtMethod behavior : hookClass.getDeclaredMethods()) {
      Hook hook = Hook.from(behavior);
      assertNotNull(hook);
    }

    for (CtBehavior behavior : targetClass.getDeclaredBehaviors()) {
      hooks.stream()
           .filter(hook -> hook.getSourceSystem().match(behavior))
           .forEach(hook -> bindings.add(new HookBinding(hook, behavior, UNUSED_PARAMETER)));
    }

    for (HookBinding hookBinding : bindings) {
      Boolean isValid = hookBinding.getHook()
                                   .getSourceSystem()
                                   .validate(hookBinding);
      assertTrue(isValid);
    }
  }
}