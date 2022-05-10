package com.appland.appmap.transform.annotations;

import java.util.Map;
import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.util.Logger;

import javassist.CtClass;
import javassist.CtBehavior;
import javassist.CtMethod;
import javassist.NotFoundException;

public abstract class SourceMethodSystem extends BaseSystem {
  public static final String EVENT_TOKEN = "$evt";

  private String hookClass;
  private String hookMethod;
  private MethodEvent methodEvent;

  protected SourceMethodSystem(CtBehavior behavior, Class<?> annotationClass) {
    super(behavior);
    this.hookClass = behavior.getDeclaringClass().getName();
    this.hookMethod = behavior.getName();
    this.methodEvent = (MethodEvent)AnnotationUtil.getObject(behavior,
      annotationClass, "methodEvent",
      MethodEvent.METHOD_INVOCATION);    
  }

  public Boolean match(CtBehavior behavior, Map<String, Object> mapResult) {
    return false;
  }

  public String getKey() {
    return null;
  }

  @Override
  public String toString() {
    return String.format("%s.%s", this.hookClass, this.hookMethod);
  }

  @Override
  public void mutateStaticParameters(CtBehavior hookBehavior, Parameters hookParameters) {
    hookParameters.add(new Value().setName(EVENT_TOKEN));
  }

  @Override
  public void mutateRuntimeParameters(HookBinding binding, Parameters runtimeParameters) {
    if (this.methodEvent == MethodEvent.METHOD_RETURN) {
      final CtBehavior targetBehavior = binding.getTargetBehavior();
      if (targetBehavior.getMethodInfo().isMethod()) {
        try {
          CtMethod method = (CtMethod) targetBehavior;
          CtClass returnType = method.getReturnType();
          Value returnValue = new Value();

          if (returnType == CtClass.voidType) {
            returnValue.setName("null");
          } else {
            returnValue.setName("com.appland.appmap.process.RuntimeUtil.boxValue($_)");
          }

          runtimeParameters.add(returnValue);
        } catch (NotFoundException e) {
          Logger.println("warning - unknown return type");
          Logger.println(e);
        }
      }
    } else if (this.methodEvent == MethodEvent.METHOD_EXCEPTION) {
      runtimeParameters.add(new Value()
          .setClassType("java.lang.Exception")
          .setName("$e"));
    }
  }

  public MethodEvent getMethodEvent() {
    return methodEvent;
  }

  @Override
  public Integer getParameterPriority() {
    return 200;
  }

  @Override
  public Integer getHookPosition() {
    final MethodEvent methodEvent = getMethodEvent();
    switch (methodEvent) {
    case METHOD_INVOCATION:
      return ISystem.HOOK_POSITION_FIRST;
    case METHOD_RETURN:
    case METHOD_EXCEPTION:
      return ISystem.HOOK_POSITION_LAST;
    default:
      // If somehow another method event type gets used, it's imperative that
      // this method get updated. So, throw an exception to indicate that
      // there's a problem.
      throw new RuntimeException("Unknown MethodEvent type (" + methodEvent.getEventString() + "," + methodEvent.getIndex() + ")");
    }
  }  
}
