package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;

import java.lang.reflect.Method;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public interface ISystem {
  public static ISystem from(CtBehavior behavior) {
    return null;
  }

  public Boolean match(CtBehavior behavior);

  public void mutateStaticParameters(CtBehavior behavior, Parameters params);
  public void mutateRuntimeParameters(HookBinding binding, Parameters runtimeParameters);

  public Integer getParameterPriority();

  public Boolean validate(Hook hook);

  public Boolean validate(HookBinding binding);
}