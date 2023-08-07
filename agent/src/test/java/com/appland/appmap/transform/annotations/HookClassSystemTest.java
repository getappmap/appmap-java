package com.appland.appmap.transform.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.test.util.ClassBuilder;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;

public class HookClassSystemTest {
  private static final String PackageName = "HookClassSystemTest";
  private static final String SuperclassName = PackageName + ".Super";
  private static final String InterfaceName = PackageName + ".IFace";
  private static final String TargetClassName = PackageName + ".TargetClass";
  private static final String HookClassName = PackageName + ".HookClass";
  private static final Integer UNUSED_PARAMETER = -1;
  private CtClass targetClass;

  @Before
  public void initializeTestClasses() throws Exception {
    CtClass iface = ClassBuilder.buildInterface(InterfaceName)
        .beginMethod()
        .setName("requiredMethod")
          .endMethod()
        .ctClass();

    CtClass[] ifaces = { iface };
    CtClass superClass = new ClassBuilder(SuperclassName).ctClass();
    superClass.setInterfaces(ifaces);

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
        .beginMethod()
          .setName("requiredMethod")
        .endMethod()
        .ctClass();
    targetClass.setSuperclass(superClass);
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
        .beginMethod()
          .setName("requiredMethod")
          .addParameter(Event.class.getName(), "event")
          .addParameter(TargetClassName, "receiver")
          .beginAnnotation(HookClass.class.getName())
            .setMember("value", InterfaceName)
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

    int bindingsSize = bindings.size();
    assertEquals("Wrong number of hooks", 4, bindingsSize);
    assertEquals(bindings.get(bindingsSize - 1).getTargetBehavior().getName(), "requiredMethod");
  }
}