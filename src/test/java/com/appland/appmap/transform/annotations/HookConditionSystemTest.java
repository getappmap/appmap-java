package com.appland.appmap.transform.annotations;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.ConfigCondition;
import static com.appland.appmap.util.StringUtil.canonicalName;
import com.appland.appmap.test.util.ClassBuilder;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class HookConditionSystemTest {
  private final static String TargetClassNameGood = "HookConditionSystemTest.Good.TargetClass";
  private final static String TargetClassNameBad = "HookConditionSystemTest.Bad.TargetClass";
  private final static String HookClassName = "HookConditionSystemTest.HookClass";
  private final static Integer UNUSED_PARAMETER = -1;
  private CtClass targetClassGood;
  private CtClass targetClassBad;

  @Before
  public void initializeTestClasses() throws Exception {
    targetClassGood = new ClassBuilder(TargetClassNameGood)
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

    targetClassBad = new ClassBuilder(TargetClassNameBad)
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

    AppMapConfig config = Mockito.spy(AppMapConfig.get());
    Mockito.doReturn(true).when(config).includes(canonicalName(TargetClassNameGood, false, "methodNoArgs"));
    Mockito.doReturn(true).when(config).includes(canonicalName(TargetClassNameGood, false, "methodSingleArg"));
    Mockito.doReturn(true).when(config).includes(canonicalName(TargetClassNameGood, false, "methodManyArgs"));
  }

  @Test
  public void testValidate() throws Exception {
    List<Hook> hooks = new ArrayList<Hook>();
    List<HookBinding> bindings = new ArrayList<HookBinding>();
    CtClass hookClass = new ClassBuilder(HookClassName)
        .beginMethod()
          .setName("methodNoArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassNameGood, "receiver")
          .beginAnnotation(HookCondition.class.getName())
            .setMember("value", ConfigCondition.class)
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodSingleArg")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassNameGood, "receiver")
          .addParameter(CtClass.intType, "x")
          .beginAnnotation(HookCondition.class.getName())
            .setMember("value", ConfigCondition.class)
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodManyArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassNameGood, "receiver")
          .addParameter(CtClass.intType, "x")
          .addParameter(CtClass.intType, "y")
          .beginAnnotation(HookCondition.class.getName())
            .setMember("value", ConfigCondition.class)
          .endAnnotation()
        .endMethod()
        .ctClass();

    for (CtMethod behavior : hookClass.getDeclaredMethods()) {
      Hook hook = Hook.from(behavior);
      assertNotNull(hook);
    }

    for (CtBehavior behavior : targetClassGood.getDeclaredBehaviors()) {
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

  public void testHookFailsCondition() throws Exception {
    List<Hook> hooks = new ArrayList<Hook>();
    List<HookBinding> bindings = new ArrayList<HookBinding>();
    CtClass hookClass = new ClassBuilder(HookClassName)
        .beginMethod()
          .setName("methodNoArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassNameBad, "receiver")
          .beginAnnotation(HookCondition.class.getName())
            .setMember("value", ConfigCondition.class)
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodSingleArg")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassNameBad, "receiver")
          .addParameter(CtClass.intType, "x")
          .beginAnnotation(HookCondition.class.getName())
            .setMember("value", ConfigCondition.class)
          .endAnnotation()
        .endMethod()
        .beginMethod()
          .setName("methodManyArgs")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassNameBad, "receiver")
          .addParameter(CtClass.intType, "x")
          .addParameter(CtClass.intType, "y")
          .beginAnnotation(HookCondition.class.getName())
            .setMember("value", ConfigCondition.class)
          .endAnnotation()
        .endMethod()
        .ctClass();

    for (CtMethod behavior : hookClass.getDeclaredMethods()) {
      Hook hook = Hook.from(behavior);
      assertNotNull(hook);
    }

    for (CtBehavior behavior : targetClassBad.getDeclaredBehaviors()) {
      hooks.stream()
          .filter(hook -> hook.getSourceSystem().match(behavior))
          .forEach(hook -> bindings.add(new HookBinding(hook, behavior, UNUSED_PARAMETER)));
    }

    for (HookBinding hookBinding : bindings) {
      Boolean isValid = hookBinding.getHook()
                                  .getSourceSystem()
                                  .validate(hookBinding);
      assertFalse(isValid);
    }
  }
}
