package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javassist.CtBehavior;

/**
 * Marshals arguments between a behavior and a hook.
 */
public class ArgumentArraySystem extends BaseSystem {
  private static final Boolean DEFAULT_VALUE = false;

  private Boolean wantsArgumentArray;

  private ArgumentArraySystem(CtBehavior hookBehavior, Boolean wantsArgumentArray) {
    super(hookBehavior);
    this.wantsArgumentArray = wantsArgumentArray;
  }

  /**
   * Factory method. Reads any relevant annotation information and caches it.
   * @param hookBehavior The hook behavior
   * @return A new ArgumentArraySystem
   */
  public static ISystem from(CtBehavior hookBehavior) {
    final Boolean wantsArgumentArray = (Boolean) AnnotationUtil.getValue(hookBehavior,
        ArgumentArray.class,
        DEFAULT_VALUE);
    return new ArgumentArraySystem(hookBehavior, wantsArgumentArray);
  }

  @Override
  public void mutateRuntimeParameters(HookBinding binding, Parameters runtimeParameters) {
    if (!this.wantsArgumentArray) {
      runtimeParameters.add(new Value().setName("$$"));
      return;
    }

    final Parameters targetParameters = binding.getTargetParameters();
    Value argArray = new Value();
    if (targetParameters.size() == 0) {
      argArray.setName("new Object[0]");
    } else {
      final String args = IntStream
          .range(1, targetParameters.size() + 1)
          .mapToObj(i -> {
            return String.format("com.appland.appmap.process.RuntimeUtil.boxValue($%d)", i);
          })
          .collect(Collectors.joining(", ", "new Object[]{ ", " }"));
      argArray.setName(args);
    }
    runtimeParameters.add(argArray);
  }

  @Override
  public Integer getParameterPriority() {
    return 300;
  }

  @Override
  public Boolean validate(Hook hook) {
    final Parameters hookParameters = hook.getParameters();
    if (this.wantsArgumentArray) {
      return hookParameters.validate(hookParameters.size() - 1, "java.lang.Object[]");
    }
    return true;
  }

  @Override
  public Boolean validate(HookBinding binding) {
    if (this.wantsArgumentArray) {
      return true;
    }

    final Parameters hookParameters = binding.getHook().getParameters();
    final Parameters targetParameters = binding.getTargetParameters();
    final Integer numParams = hookParameters.size(); // 2
    final Integer startIndex = hookParameters.size() - targetParameters.size(); // 2

    if (numParams - startIndex != targetParameters.size()) { // 0
      return false;
    }

    try {
      for (int i = startIndex; i < numParams; ++i) {
        Value hookParam = hookParameters.get(i);
        if (!targetParameters.validate(i - startIndex, hookParam.classType)) {
          return false;
        }
      }
    } catch (NoSuchElementException e) {
      return false;
    }

    return true;
  }
}