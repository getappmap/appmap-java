package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;
import javassist.CtBehavior;

/**
 * Responsible for runtime processing of the annotations which cause hooking to occur.
 * For each type of hooking annotation, there is a corresponding ISystem. The ISystem
 * configures the hooking behavior for that particular annotation.
 *
 * For example, adding the ArgumentArray annotation to a hook method activates the ArgumentArraySystem.
 * ArgumentArraySystem ensures that the event arguments will be passed to the hook method as Object[].
 * This overrides the default behavior, in which arguments are passed according to the method signature.
 *
 * @see ArgumentArray
 */
public interface ISystem {
  int HOOK_POSITION_FIRST = -1;
  int HOOK_POSITION_DEFAULT = 0;
  int HOOK_POSITION_LAST = 1;

  public static ISystem from(CtBehavior behavior) {
    return null;
  }

  public Boolean match(CtBehavior behavior);

  public void mutateStaticParameters(CtBehavior behavior, Parameters params);

  public void mutateRuntimeParameters(HookBinding binding, Parameters runtimeParameters);

  public Integer getHookPosition();

  public Integer getParameterPriority();

  public Boolean validate(Hook hook);

  public Boolean validate(HookBinding binding);
}
