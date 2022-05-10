package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.test.util.ClassBuilder;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class ArgumentArraySystemTest {
  private final static String TargetClassName = "ArgumentArraySystemTest.TargetClass";
  private final static String HookClassName = "ArgumentArraySystemTest.HookClass";
  private final static Integer UNUSED_PARAMETER = -1;
  private CtClass targetClass;

  @Before
  public void initializeTestClasses() throws Exception {
    targetClass = new ClassBuilder(TargetClassName)
        .beginMethod()
          .setName("methodNoArgs")
        .endMethod()
        .beginMethod()
          .setName("methodSingleArg")
          .beginParameter()
            .setType(CtClass.intType)
            .setId("x")
          .endParameter()
        .endMethod()
        .beginMethod()
          .setName("methodManyArgs")
          .beginParameter()
            .setType(CtClass.intType)
            .setId("x")
          .endParameter()
          .beginParameter()
            .setType(CtClass.intType)
            .setId("y")
          .endParameter()
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
          .addParameter("java.lang.Object[]", "args")
          .beginAnnotation()
            .setType(HookClass.class.getName())
            .setMember("value", TargetClassName)
          .endAnnotation()
          .addAnnotation(ArgumentArray.class.getName())
        .endMethod()
        .beginMethod()
          .setName("methodSingleArg")
          .addParameter(Event.class.getName(), "event")
          .addParameter("java.lang.Object[]", "args")
          .beginAnnotation()
            .setType(HookClass.class.getName())
            .setMember("value", TargetClassName)
          .endAnnotation()
          .addAnnotation(ArgumentArray.class.getName())
        .endMethod()
        .beginMethod()
          .setName("methodManyArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter("java.lang.Object[]", "args")
          .beginAnnotation()
            .setType(HookClass.class.getName())
            .setMember("value", TargetClassName)
          .endAnnotation()
          .addAnnotation(ArgumentArray.class.getName())
        .endMethod()
        .ctClass();

    for (CtMethod behavior : hookClass.getDeclaredMethods()) {
      Hook hook = Hook.from(behavior);
      assertNotNull(hook);
      hooks.add(hook);
    }

    for (CtBehavior behavior : targetClass.getDeclaredBehaviors()) {
      Map<String, Object> matchResult = new HashMap<String, Object>();
      hooks.stream()
           .filter(hook -> hook.getSourceSystem().match(behavior, matchResult))
           .forEach(hook -> bindings.add(new HookBinding(hook, behavior, UNUSED_PARAMETER)));
    }

    for (HookBinding hookBinding : bindings) {
      Boolean isValid = hookBinding.getHook()
                                   .getSystem(ArgumentArraySystem.class)
                                   .validate(hookBinding);
      assertTrue(isValid);
    }
  }

  @Test
  public void testInvalidHooksFailStaticValidation() throws Exception {
    CtClass hookClass = new ClassBuilder(HookClassName)
        .beginMethod()
          .setName("methodManyArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter("java.lang.String", "invalidArg")
          .beginAnnotation()
            .setType(HookClass.class.getName())
            .setMember("value", TargetClassName)
          .endAnnotation()
          .addAnnotation(ArgumentArray.class.getName())
        .endMethod()
        .ctClass();

    CtMethod invalidHookMethod = hookClass.getDeclaredMethods()[0];
    assertNull(Hook.from(invalidHookMethod));
  }
}
