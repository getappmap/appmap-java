package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;

import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;

import javassist.CtBehavior;

public class ExcludeReceiverSystem extends BaseSystem {
  private static final Boolean DEFAULT_VALUE = false;

  private Boolean excludeReceiver;

  private ExcludeReceiverSystem(CtBehavior hookBehavior, Boolean excludeReceiver) {
    super(hookBehavior);
    this.excludeReceiver = excludeReceiver;
  }

  /**
   * Factory method. Reads any relevant annotation information and caches it.
   * @param behavior The hook behavior
   * @return A new ExcludeReceiverSystem
   */
  public static ISystem from(CtBehavior behavior) {
    Boolean doesExcludeReceiver = (Boolean) AnnotationUtil.getValue(behavior,
        ExcludeReceiver.class,
        DEFAULT_VALUE);
    return new ExcludeReceiverSystem(behavior, doesExcludeReceiver);
  }

  @Override
  public void mutateStaticParameters(CtBehavior hookBehavior, Parameters hookParams) {
    if (!this.excludeReceiver) {
      Value receiver = new Value();
      if (Modifier.isStatic(hookBehavior.getModifiers())) {
        receiver.setName("null");
      } else {
        receiver.setName("this");
      }
      hookParams.add(receiver);
    }
  }

  @Override
  public Integer getParameterPriority() {
    return 100;
  }

  @Override
  public Boolean validate(HookBinding binding) {
    if (!this.excludeReceiver) {
      Parameters hookParameters = binding.getHook().getParameters();
      try {
        Value receiverType = hookParameters.get(1);
        return CtClassUtil.isChildOf(binding.getTargetBehavior().getDeclaringClass(),
            receiverType.classType);
      } catch (NoSuchElementException e) {
        return false;
      }
    }
    return true;
  }
}