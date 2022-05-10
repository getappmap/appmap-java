package com.appland.appmap.transform.annotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.test.util.ClassBuilder;
import com.appland.appmap.test.util.MethodBuilder;
import com.appland.appmap.util.FullyQualifiedName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;

public class HookConditionSystemTest {
  private final static String TargetClassNameGood = "HookConditionSystemTest.Good.TargetClass";
  private final static String TargetClassNameBad = "HookConditionSystemTest.Bad.TargetClass";
  private final static String HookClassName = "HookConditionSystemTest.HookClass";
  private final static Integer UNUSED_PARAMETER = -1;
  private CtClass targetClassGood;
  private CtClass targetClassBad;

  @Before
  public void initializeTestClasses() throws Exception {
    final ClassBuilder goodClass = new ClassBuilder(TargetClassNameGood);
    final MethodBuilder methodNoArgs = goodClass.beginMethod();
    methodNoArgs
      .setName("methodNoArgs")
      .addAnnotation(Test.class.getName())
    .endMethod();

    final MethodBuilder methodSingleArg = goodClass.beginMethod();
    methodSingleArg
      .setName("methodSingleArg")
      .addParameter(CtClass.intType, "x")
      .addAnnotation(Test.class.getName())
    .endMethod();

    final MethodBuilder methodManyArgs = goodClass.beginMethod();
    methodManyArgs
      .setName("methodManyArgs")
      .addParameter(CtClass.intType, "x")
      .addParameter(CtClass.intType, "y")
      .addAnnotation(Test.class.getName())
    .endMethod();

    targetClassGood = goodClass.ctClass();

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
    AppMapPackage.LabelConfig noLabels = new AppMapPackage.LabelConfig();
    Mockito.doReturn(noLabels).when(config).includes(new FullyQualifiedName(methodNoArgs.getBehavior()));
    Mockito.doReturn(noLabels).when(config).includes(new FullyQualifiedName(methodSingleArg.getBehavior()));
    Mockito.doReturn(noLabels).when(config).includes(new FullyQualifiedName(methodManyArgs.getBehavior()));
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
      Map<String, Object> matchResult = new HashMap<String, Object>();
      hooks.stream()
          .filter(hook -> hook.getSourceSystem().match(behavior, matchResult))
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
