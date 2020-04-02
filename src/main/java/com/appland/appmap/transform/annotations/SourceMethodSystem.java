package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;

import javassist.CtBehavior;

public abstract class SourceMethodSystem extends BaseSystem {
  public static final String EVENT_TOKEN = "$evt";

  private String hookClass;
  private String hookMethod;

  protected SourceMethodSystem(CtBehavior behavior) {
    super(behavior);
    this.hookClass = behavior.getDeclaringClass().getName();
    this.hookMethod = behavior.getName();
  }

  public Boolean match(CtBehavior behavior) {
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
  public Integer getParameterPriority() {
    return Integer.MIN_VALUE;
  }
}