package com.appland.appmap.transform.annotations;

import java.util.Map;
import com.appland.appmap.output.v1.Parameters;
import javassist.CtBehavior;

public abstract class BaseSystem implements ISystem {
  private final CtBehavior hookBehavior;

  public BaseSystem(CtBehavior hookBehavior) {
    this.hookBehavior = hookBehavior;
  }

  public Boolean match(CtBehavior behavior, Map<String, Object> matchResult) {
    return false;
  }

  public void mutateStaticParameters(CtBehavior behavior, Parameters params) {

  }

  public void mutateRuntimeParameters(HookBinding binding, Parameters runtimeParameters) {

  }

  protected CtBehavior getHookBehavior() {
    return this.hookBehavior;
  }

  public Integer getHookPosition() {
    return ISystem.HOOK_POSITION_DEFAULT;
  }

  public Integer getParameterPriority() {
    return Integer.MAX_VALUE;
  }

  public Boolean validate(Hook hook) {
    return true;
  }

  public Boolean validate(HookBinding binding) {
    return true;
  }
}
