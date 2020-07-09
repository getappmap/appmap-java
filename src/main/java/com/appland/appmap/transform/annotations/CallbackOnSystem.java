package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.util.Logger;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class CallbackOnSystem extends BaseSystem {
  private static final MethodEvent DEFAULT_VALUE = MethodEvent.METHOD_INVOCATION;

  private MethodEvent methodEvent;

  private CallbackOnSystem(CtBehavior hookBehavior, MethodEvent methodEvent) {
    super(hookBehavior);
    this.methodEvent = methodEvent;
  }

  /**
   * Factory method. Reads any relevant annotation information and caches it.
   * @param behavior The hook behavior
   * @return A new CallbackOnSystem
   */
  public static ISystem from(CtBehavior behavior) {
    MethodEvent methodEvent = (MethodEvent) AnnotationUtil.getValue(behavior,
        CallbackOn.class,
        DEFAULT_VALUE);
    return new CallbackOnSystem(behavior, methodEvent);
  }

  public MethodEvent getMethodEvent() {
    return this.methodEvent;
  }

  @Override
  public void mutateRuntimeParameters(HookBinding binding, Parameters runtimeParameters) {
    if (this.methodEvent == MethodEvent.METHOD_RETURN) {
      final CtBehavior targetBehavior = binding.getTargetBehavior();
      if (targetBehavior instanceof CtMethod) {
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
          Logger.println(e.getMessage());
        }
      }
    } else if (this.methodEvent == MethodEvent.METHOD_EXCEPTION) {
      runtimeParameters.add(new Value()
          .setClassType("java.lang.Exception")
          .setName("$e"));
    }
  }

  @Override
  public Integer getParameterPriority() {
    return 200;
  }
}
