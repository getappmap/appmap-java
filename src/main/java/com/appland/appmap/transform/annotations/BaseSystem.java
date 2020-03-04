package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;

import java.lang.reflect.Method;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public abstract class BaseSystem implements ISystem {
  private final CtBehavior hookBehavior;

  public BaseSystem(CtBehavior hookBehavior) {
    this.hookBehavior = hookBehavior;
  }

  public Boolean match(CtBehavior behavior) {
    return false;
  }

  public void mutateStaticParameters(CtBehavior behavior, Parameters params) {

  }

  public void mutateRuntimeParameters(HookBinding binding, Parameters runtimeParameters) {

  }

  protected CtBehavior getHookBehavior() {
    return this.hookBehavior;
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

  public ParameterType getParameterType() {
    return null;
  }
}