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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HookClassSystemTest {
  private final static String TargetClassName = "HookClassSystemTest.TargetClass";
  private final static String HookClassName = "HookClassSystemTest.HookClass";
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
          .beginAnnotation(HookClass.class.getName())
            .setMember("value", TargetClassName)
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodSingleArg")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassName, "receiver")
          .addParameter(CtClass.intType, "x")
          .beginAnnotation(HookClass.class.getName())
            .setMember("value", TargetClassName)
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodManyArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassName, "receiver")
          .addParameter(CtClass.intType, "x")
          .addParameter(CtClass.intType, "y")
          .beginAnnotation(HookClass.class.getName())
            .setMember("value", TargetClassName)
          .endAnnotation()
        .endMethod()
        .ctClass();

    for (CtMethod behavior : hookClass.getDeclaredMethods()) {
      Hook hook = Hook.from(behavior);
      assertNotNull(hook);
      hooks.add(hook);
    }

    assertTrue("No hooks?", hooks.size() > 0);

    for (CtBehavior behavior : targetClass.getDeclaredBehaviors()) {
      Map<String, Object> matchResult = new HashMap<String, Object>();
      hooks.stream()
           .filter(hook -> hook.getSourceSystem().match(behavior, matchResult))
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