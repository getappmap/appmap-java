package com.appland.appmap.transform.annotations;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.EventTemplateRegistry;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;
import javassist.expr.MethodCall;

public class Hook {
  private static final EventTemplateRegistry eventTemplateRegistry = EventTemplateRegistry.get();

  private final static List<Function<CtBehavior, ISystem>> requiredHookSystemFactories =
  new ArrayList<Function<CtBehavior, ISystem>>() {{
      add(HookAnnotatedSystem::from);
      add(HookClassSystem::from);
      add(HookConditionSystem::from);
  }};

  private final static List<Function<CtBehavior, ISystem>> optionalSystemFactories =
      new ArrayList<Function<CtBehavior, ISystem>>() {{
          add(CallbackOnSystem::from);
          add(ExcludeReceiverSystem::from);
          add(ArgumentArraySystem::from);
      }};

  private SourceMethodSystem sourceSystem;
  private List<ISystem> optionalSystems;
  private Parameters staticParameters = new Parameters();
  private Parameters hookParameters;
  private CtBehavior hookBehavior;
  private Boolean continueHooking = false;
  private String uniqueKey = "";

  private Hook( SourceMethodSystem sourceSystem,
                List<ISystem> optionalSystems,
                CtBehavior hookBehavior) {
    this.sourceSystem = sourceSystem;
    this.optionalSystems = optionalSystems;
    this.hookBehavior = hookBehavior;
    this.hookParameters = new Parameters(hookBehavior);

    this.continueHooking = (Boolean) AnnotationUtil.getValue(hookBehavior,
        ContinueHooking.class,
        false);

    this.uniqueKey = (String) AnnotationUtil.getValue(hookBehavior, Unique.class, "");

    this.buildParameters();
  }

  public static Hook from(CtBehavior hookBehavior) {
    SourceMethodSystem sourceSystem = null;
    for (Function<CtBehavior, ISystem> factoryFn : requiredHookSystemFactories) {
      sourceSystem = (SourceMethodSystem) factoryFn.apply(hookBehavior);
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
        System.err.println("AppMap: hook "
            + hook
            + " failed validation from "
            + optionalSystem.getClass().getSimpleName());
        return null;
      }
    }

    return hook;
  }

  public void buildParameters() {
    this.sourceSystem.mutateStaticParameters(this.hookBehavior, this.staticParameters);
    this.optionalSystems
        .stream()
        .sorted(Comparator.comparingInt(ISystem::getParameterPriority))
        .forEach(system -> system.mutateStaticParameters(this.hookBehavior, this.staticParameters));
  }

  public Parameters getRuntimeParameters(HookBinding binding) {
    Parameters runtimeParameters = this.staticParameters.clone();
    this.sourceSystem.mutateRuntimeParameters(binding, runtimeParameters);
    this.optionalSystems
        .stream()
        .sorted(Comparator.comparingInt(ISystem::getParameterPriority))
        .forEach(system -> {
          system.mutateRuntimeParameters(binding, runtimeParameters);
        });
    return runtimeParameters;
  }

  public HookSite prepare(CtBehavior targetBehavior) {
    if (targetBehavior instanceof CtConstructor) {
      return null;
    }

    if (!this.sourceSystem.match(targetBehavior)) {
      return null;
    }

    Integer behaviorOrdinal = eventTemplateRegistry.register(targetBehavior);
    if (behaviorOrdinal < 0) {
      return null;
    }

    HookBinding binding = new HookBinding(this, targetBehavior, behaviorOrdinal);
    Parameters behaviorParameters = new Parameters(targetBehavior);
    for (ISystem system : this.optionalSystems) {
      if (!system.validate(binding)) {
        return null;
      }
    }

    Parameters runtimeParameters = this.getRuntimeParameters(binding);

    return new HookSite(this, behaviorOrdinal, runtimeParameters);

    // String hookSource = this.getInvocationSource(behaviorOrdinal, methodEvent, runtimeParameters);

    // try {
    //   if (methodEvent == MethodEvent.METHOD_INVOCATION) {
    //     hookSource = this.wrapTryCatchSource(behavior, hookSource);
    //     behavior.insertBefore(hookSource);
    //   } else if (methodEvent == MethodEvent.METHOD_RETURN) {
    //     behavior.insertAfter(hookSource);
    //   } else if (methodEvent == MethodEvent.METHOD_EXCEPTION) {
    //     Value exceptionValue = runtimeParameters.get(2);

    //     hookSource = String.format("%s throw %s;", hookSource, exceptionValue.name);
    //     behavior.addCatch(hookSource,
    //         ClassPool.getDefault().get("java.lang.Throwable"),
    //         exceptionValue.name);
    //   }
    // } catch (CannotCompileException e) {
    //   System.err.printf("AppMap: failed to compile\n");
    //   System.err.printf("        hook   %s\n", this.toString());
    //   System.err.printf("        method %s.%s\n",
    //       behavior.getDeclaringClass().getName(),
    //       behavior.getName());
    //   System.err.println(e.getMessage());
    //   System.err.println(hookSource);

    // } catch (Exception e) {
    //   System.err.println("AppMap: failed to apply hook");
    //   System.err.printf("%s: %s\n", e.getClass(), e.getMessage());
    //   e.printStackTrace(System.err);
    //   return false;
    // }

    // return true;
  }

  private static String wrapQuotes(String val) {
    if (val.isEmpty()) {
      return "";
    }

    return "\"" + val + "\"";
  }

  private static void addBooleanField(CtBehavior behavior, String name, boolean defaultValue)
      throws CannotCompileException {
    final CtField field = CtField.make("private static transient boolean " + name + " = " + defaultValue + ";",
        behavior.getDeclaringClass());

    behavior.getDeclaringClass().addField(field);
  }

  public static void apply(CtBehavior targetBehavior, List<HookSite> hookSites) {
    final String globalLock = RandomIdentifier.build("globalLock");
    final CtClass returnType = getReturnType(targetBehavior);
    final Boolean returnsVoid = (returnType == CtClass.voidType);

    final HashMap<String, String> uniqueLocks = new HashMap<String, String>();
    final String calls = hookSites
        .stream()
        .filter(hookSite -> hookSite.getMethodEvent() == MethodEvent.METHOD_INVOCATION)
        .map(hookSite -> {
          if (hookSite.getUniqueKey().isEmpty()) {
            return (""
                + globalLock + " = com.appland.appmap.process.ThreadLock.current().acquireLock();"
                + "if (" + globalLock + ") {"
                +   hookSite.getHookInvocation()
                +   "com.appland.appmap.process.ThreadLock.current().releaseLock();"
                +   globalLock + " = false;"
                + "}");
          }

          final String uniqueKey = hookSite.getUniqueKey();
          String lockId = uniqueLocks.get(uniqueKey);

          if (lockId == null) {
            lockId = RandomIdentifier.build("uniqueLock");
            uniqueLocks.put(uniqueKey, lockId);
          }

          return (""
              + globalLock + " = " + lockId + " = com.appland.appmap.process.ThreadLock.current().acquireLock(" + wrapQuotes(uniqueKey) + "); "
              + "if (" + lockId + ") {"
              +   hookSite.getHookInvocation()
              +   "com.appland.appmap.process.ThreadLock.current().releaseLock(); "
              +   globalLock + " = false;"
              + "}");
        })
        .collect(Collectors.joining("\n", "{ ", " }"));

    final String returns = hookSites
        .stream()
        .filter(hookSite -> hookSite.getMethodEvent() == MethodEvent.METHOD_RETURN)
        .map(hookSite -> {
          if (hookSite.getUniqueKey().isEmpty()) {
            return (""
                + globalLock + " = com.appland.appmap.process.ThreadLock.current().acquireLock();"
                + "if (" + globalLock + ") {"
                +   hookSite.getHookInvocation()
                +   "com.appland.appmap.process.ThreadLock.current().releaseLock();"
                +   globalLock + " = false;"
                + "}");
          }

          final String uniqueKey = hookSite.getUniqueKey();
          String lockId = uniqueLocks.get(uniqueKey);

          if (lockId == null) {
            lockId = RandomIdentifier.build("uniqueLock");
            uniqueLocks.put(uniqueKey, lockId);
          }

          return (""
              + globalLock + " = com.appland.appmap.process.ThreadLock.current().acquireLock();"
              + "if (" + globalLock + ") {"
              +   "if (" + lockId + ") {"
              +     hookSite.getHookInvocation()
              +   "} else {"
              +     lockId + " = com.appland.appmap.process.ThreadLock.current().acquireUniqueLock(" + wrapQuotes(uniqueKey) + ");"
              +     "if (" + lockId + ") {"
              +       hookSite.getHookInvocation()
              +     "}"
              +   "}"
              +   "com.appland.appmap.process.ThreadLock.current().releaseLock();"
              +   globalLock + " = false;"
              + "}");
        })
        .collect(Collectors.joining("\n", "{ ", " }"));

    final String exceptions = hookSites
        .stream()
        .filter(hookSite -> hookSite.getMethodEvent() == MethodEvent.METHOD_EXCEPTION)
        .map(hookSite -> {
          if (hookSite.getUniqueKey().isEmpty()) {
            return (""
                + globalLock + " = com.appland.appmap.process.ThreadLock.current().acquireLock();"
                + "if (" + globalLock + ") {"
                +   hookSite.getHookInvocation()
                +   "com.appland.appmap.process.ThreadLock.current().releaseLock();"
                +   globalLock + " = false;"
                + "}");
          }

          final String uniqueKey = hookSite.getUniqueKey();
          String lockId = uniqueLocks.get(uniqueKey);

          if (lockId == null) {
            lockId = RandomIdentifier.build("uniqueLock");
            uniqueLocks.put(uniqueKey, lockId);
          }

          return (""
              + globalLock + " = com.appland.appmap.process.ThreadLock.current().acquireLock();"
              + "if (" + globalLock + ") {"
              +   "if (" + lockId + ") {"
              +     hookSite.getHookInvocation()
              +   "} else {"
              +     lockId + " = com.appland.appmap.process.ThreadLock.current().acquireUniqueLock(" + wrapQuotes(uniqueKey) + ");"
              +     "if (" + lockId + ") {"
              +       hookSite.getHookInvocation()
              +     "}"
              +   "}"
              +   "com.appland.appmap.process.ThreadLock.current().releaseLock();"
              +   globalLock + " = false;"
              + "}");
        })
        .collect(Collectors.joining("\n", "{ ", " }"));

    final String unlockUniques = uniqueLocks
          .entrySet()
          .stream()
          .map(e -> {
            final String lockId = e.getValue();
            return (""
                + "if (" + lockId + ") {"
                +   "com.appland.appmap.process.ThreadLock.current().releaseUniqueLock(" + wrapQuotes(e.getKey()) + ");"
                +   lockId + " = false;"
                + "}"
                + "if (" + globalLock + ") {"
                +   "com.appland.appmap.process.ThreadLock.current().releaseLock();"
                +   globalLock + " = false;"
                + "}");
          })
          .collect(Collectors.joining("\n", "{ ", " }"));

    try {
      addBooleanField(targetBehavior, globalLock, false);

      for (String variableId : uniqueLocks.values()) {
        addBooleanField(targetBehavior, variableId, false);
      }

      targetBehavior.insertAfter(returns);
      targetBehavior.insertBefore(calls);

      if (returnsVoid) {
        targetBehavior.addCatch("return;",
            ClassPool.getDefault().get("com.appland.appmap.process.ExitEarly"));
      } else if (!returnType.isPrimitive()) {
        targetBehavior.addCatch("return (" + returnType.getName() + ") $e.getReturnValue();",
            ClassPool.getDefault().get("com.appland.appmap.process.ExitEarly"));
      }

      targetBehavior.addCatch("{" + exceptions + "throw $e;}", ClassPool.getDefault().get("java.lang.Throwable"));
      targetBehavior.insertAfter(unlockUniques, true);
    } catch (CannotCompileException e) {
      System.err.println("AppMap: failed to compile");
      System.err.println("        method "
          + targetBehavior.getDeclaringClass().getName()
          + "."
          + targetBehavior.getName());
      // System.err.println("calls:     " + calls);
      // System.err.println("returns:   " + returns);
      // System.err.println("unlocks:   " + unlockUniques);
      System.err.println(e.getMessage());
    } catch (NotFoundException e) {
      System.err.println("AppMap: failed to find class\n");
      System.err.println(e.getMessage());
    }
  }

  public String getKey() {
    return this.sourceSystem.getKey();
  }

  public String toString() {
    return String.format("%s(%s)", this.sourceSystem.toString(), this.hookParameters.toString());
  }

  public String getUniqueKey() {
    return this.uniqueKey;
  }

  public Parameters getParameters() {
    return this.hookParameters;
  }

  public Integer getParameterIndex(ParameterType parameterType) {
    return 0;
  }
  
  public void validate() throws HookValidationException {

  } 

  public void validate(CtBehavior behavior) throws HookValidationException {

  }

  public MethodEvent getMethodEvent() {
    MethodEvent methodEvent = MethodEvent.METHOD_INVOCATION;
    for (ISystem system : this.optionalSystems) {
      if (system instanceof CallbackOnSystem) {
        methodEvent = ((CallbackOnSystem) system).getMethodEvent();
        break;
      }
    }
    return methodEvent;
  }

  public SourceMethodSystem getSourceSystem() {
    return this.sourceSystem;
  }

  private static CtClass getReturnType(CtBehavior behavior) {
    CtClass returnType = CtClass.voidType;
    if (behavior instanceof CtMethod) {
      try {
        returnType = ((CtMethod) behavior).getReturnType();
      } catch (NotFoundException e) {
        System.err.println("AppMap: warning - unknown return type");
        System.err.println(e.getMessage());
      }
    }
    return returnType;
  }
}