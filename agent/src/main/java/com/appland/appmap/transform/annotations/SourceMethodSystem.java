package com.appland.appmap.transform.annotations;

import java.util.Map;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public abstract class SourceMethodSystem extends BaseSystem {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

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
          // getReturnType throws a NotFoundException when the class of the
          // returned value can't be found. See the note in the Parameters
          // constructor describing why we should be able to ignore this.
          logger.debug(e, "unknown return type");
        }
      }
    } else if (this.methodEvent == MethodEvent.METHOD_EXCEPTION) {
      runtimeParameters.add(new Value()
          .setClassType("java.lang.Throwable")
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
