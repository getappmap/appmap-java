package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;

import javassist.CtBehavior;

/**
 * Represents the relationship between a hook and a behavior to be transformed.
 */
public class HookBinding {
  private final Hook hook;
  private final CtBehavior targetBehavior;
  private final Parameters targetParameters;
  private final Integer behaviorOrdinal;

  public HookBinding(Hook hook, CtBehavior targetBehavior, Integer behaviorOrdinal) {
    this.hook = hook;
    this.targetBehavior = targetBehavior;
    this.targetParameters = new Parameters(this.targetBehavior);
    this.behaviorOrdinal = behaviorOrdinal;
  }

  public Hook getHook() {
    return this.hook;
  }

  public CtBehavior getTargetBehavior() {
    return this.targetBehavior;
  } 

  public Parameters getTargetParameters() {
    return this.targetParameters;
  }
}