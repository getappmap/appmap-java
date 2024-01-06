package com.appland.appmap.transform.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.appland.appmap.util.Logger;

import javassist.CtBehavior;

public class HookFactory {

  private static final List<Function<CtBehavior, ISystem>> AGENT_METHOD_FACTORIES =
      new ArrayList<Function<CtBehavior, ISystem>>() {
        {
          add(HookAnnotatedSystem::from);
          add(HookClassSystem::from);
        }
      };

  private static final List<Function<CtBehavior, ISystem>> APP_METHOD_FACTORIES =
      new ArrayList<Function<CtBehavior, ISystem>>() {
        {
          add(HookConditionSystem::from);
        }
      };

  private static final List<Function<CtBehavior, ISystem>> ALL_METHOD_FACTORIES =
      new ArrayList<Function<CtBehavior, ISystem>>() {
        {
          addAll(AGENT_METHOD_FACTORIES);
          addAll(APP_METHOD_FACTORIES);
        }
      };

  public static final HookFactory AGENT_HOOKS_FACTORY = new HookFactory(AGENT_METHOD_FACTORIES);
  public static final HookFactory APP_HOOKS_FACTORY = new HookFactory(APP_METHOD_FACTORIES);
  public static final HookFactory ALL_HOOKS_FACTORY = new HookFactory(ALL_METHOD_FACTORIES);

  private List<Function<CtBehavior, ISystem>> requiredHookSystemFactories = new ArrayList<>();
  private final static List<Function<CtBehavior, ISystem>> optionalSystemFactories =
      new ArrayList<Function<CtBehavior, ISystem>>() {
        {
          add(ExcludeReceiverSystem::from);
          add(ArgumentArraySystem::from);
        }
      };


  private HookFactory(List<Function<CtBehavior, ISystem>> hookSystemFactories) {
    requiredHookSystemFactories.addAll(hookSystemFactories);
  }

  /**
   * Creates a Hook from code behavior.
   *
   * @return a new Hook object if hookBehavior is a valid hook, null otherwise
   */
  public Hook from(CtBehavior hookBehavior) {
    SourceMethodSystem sourceSystem = null;
    for (Function<CtBehavior, ISystem> factoryFn : requiredHookSystemFactories) {
      sourceSystem = (SourceMethodSystem)factoryFn.apply(hookBehavior);
      if (sourceSystem != null) {
        break;
      }
    }

    if (sourceSystem == null) {
      return null;
    }

    List<ISystem> optionalSystems = optionalSystemFactories
        .stream()
        .map(factoryFn -> factoryFn.apply(hookBehavior))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    Hook hook = new Hook(sourceSystem, optionalSystems, hookBehavior);
    for (ISystem optionalSystem : optionalSystems) {
      if (!optionalSystem.validate(hook)) {
        Logger.println("hook "
            + hook
            + " failed validation from "
            + optionalSystem.getClass().getSimpleName());
        return null;
      }
    }

    return hook;
  }
}
