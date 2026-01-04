package com.appland.appmap.transform.annotations;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.record.EventTemplateRegistry;
import com.appland.appmap.transform.annotations.AnnotationUtil.AnnotatedBehavior;
import com.appland.appmap.util.AppMapClassPool;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.MemberValue;

public class Hook {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private static final EventTemplateRegistry eventTemplateRegistry = EventTemplateRegistry.get();



  private final SourceMethodSystem sourceSystem;
  private final List<ISystem> optionalSystems;
  private final Parameters staticParameters = new Parameters();
  private final Parameters hookParameters;
  private final CtBehavior hookBehavior;
  private String uniqueKey = "";

  public static final String ANNOTATIONS = "annotations";

  Hook(SourceMethodSystem sourceSystem,
                List<ISystem> optionalSystems,
                CtBehavior hookBehavior) {
    this.sourceSystem = sourceSystem;
    this.optionalSystems = optionalSystems;
    this.hookBehavior = hookBehavior;
    this.hookParameters = new Parameters(hookBehavior);
    this.uniqueKey = (String) AnnotationUtil.getValue(hookBehavior, Unique.class, "");

    this.buildParameters();
  }



  public void buildParameters() {
    this.sourceSystem.mutateStaticParameters(this.hookBehavior, this.staticParameters);
    this.optionalSystems
        .stream()
        .sorted(Comparator.comparingInt(ISystem::getParameterPriority))
        .forEach(system -> system.mutateStaticParameters(this.hookBehavior, this.staticParameters));
  }

  public Parameters getRuntimeParameters(HookBinding binding) {
    Parameters runtimeParameters = this.staticParameters.freshCopy();
    Stream.concat(Stream.of(this.sourceSystem), this.optionalSystems.stream())
        .sorted(Comparator.comparingInt(ISystem::getParameterPriority))
        .forEach(system -> {
          system.mutateRuntimeParameters(binding, runtimeParameters);
        });
    return runtimeParameters;
  }

  public HookSite prepare(CtBehavior targetBehavior, Map<String, Object> hookContext) {
    if (targetBehavior instanceof CtConstructor) {
      return null;
    }

    if (!this.sourceSystem.match(targetBehavior, hookContext)) {
      return null;
    }

    String[] labels = (String[])hookContext.getOrDefault("labels", new String[0]);
    Integer behaviorOrdinal = eventTemplateRegistry.register(targetBehavior, labels);
    if (behaviorOrdinal < 0) {
      return null;
    }

    HookBinding binding = new HookBinding(this, targetBehavior, behaviorOrdinal);
    for (ISystem system : this.optionalSystems) {
      if (!system.validate(binding)) {
        return null;
      }
    }

    return new HookSite(this, behaviorOrdinal, binding);
  }

  public static void apply(CtBehavior targetBehavior, List<HookSite> hookSites) {
    MethodInfo methodInfo = targetBehavior.getMethodInfo();
    AnnotationsAttribute attr =
        (AnnotationsAttribute)methodInfo.getAttribute(AnnotationsAttribute.visibleTag);

    // If the behavior is marked as an app method, update the annotation with
    // the behavior ordinals so the bytebuddy transformer can instrument it.
    if (attr.getAnnotation(AppMapAppMethod.class.getName()) != null) {
      setBehaviorOrdinals(targetBehavior, hookSites);
    }

    // If it's (also) marked as an agent method, it needs to be instrumented
    // by javassist.
    if (attr.getAnnotation(AppMapAgentMethod.class.getName()) != null) {
      instrument(targetBehavior, hookSites);
    }
  }

  public static void instrument(CtBehavior targetBehavior, List<HookSite> hookSites) {
    final CtClass returnType = getReturnType(targetBehavior);
    final Boolean returnsVoid = (returnType == CtClass.voidType);

    StringBuilder uniqueLocks = new StringBuilder();
    Set<String> uniqueKeys = new HashSet<>();
    final String[] invocations = new String[3];
    for (HookSite hookSite : hookSites) {
      final Integer index = hookSite.getMethodEvent().getIndex();
      if (invocations[index] == null) {
        invocations[index] = hookSite.getHookInvocation();
      } else {
        invocations[index] += hookSite.getHookInvocation();
      }
      String uniqueKey = hookSite.getUniqueKey();
      if (!uniqueKey.isEmpty()) {
        if (!uniqueKeys.contains(uniqueKey)) {
          uniqueLocks.append("com.appland.appmap.process.ThreadLock.current().lockUnique(\"")
              .append(uniqueKey).append("\");");
          uniqueKeys.add(uniqueKey);
        }
      }

    }

    try {
      String beforeSrcBlock = beforeSrcBlock(uniqueLocks.toString(),
          invocations[MethodEvent.METHOD_INVOCATION.getIndex()]);
      logger.trace("{}: beforeSrcBlock:\n{}", targetBehavior::getName, beforeSrcBlock::toString);
      targetBehavior.insertBefore(
          beforeSrcBlock);

      String afterSrcBlock = afterSrcBlock(invocations[MethodEvent.METHOD_RETURN.getIndex()]);
      logger.trace("{}: afterSrcBlock:\n{}", targetBehavior::getName, afterSrcBlock::toString);

      targetBehavior.insertAfter(
          afterSrcBlock);

      ClassPool cp = AppMapClassPool.get();
      String exitEarlyCatchSrc = "{com.appland.appmap.process.ThreadLock.current().exit();return;}";
      if (returnsVoid) {
        targetBehavior.addCatch(exitEarlyCatchSrc,
            cp.get("com.appland.appmap.process.ExitEarly"));
      } else if (!returnType.isPrimitive()) {
        exitEarlyCatchSrc = "{com.appland.appmap.process.ThreadLock.current().exit();return("
            + returnType.getName() + ")$e.getReturnValue();}";
        targetBehavior
            .addCatch(exitEarlyCatchSrc, cp.get("com.appland.appmap.process.ExitEarly"));
      }
      logger.trace("{}: catch1Src:\n{}", targetBehavior::getName, exitEarlyCatchSrc::toString);

      String catchSrcBlock = catchSrcBlock(invocations[MethodEvent.METHOD_EXCEPTION.getIndex()]);
      targetBehavior.addCatch(
          catchSrcBlock,
          cp.get("java.lang.Throwable"));
      logger.trace("{}: catchSrcBlock:\n{}", targetBehavior::getName, catchSrcBlock::toString);

    } catch (CannotCompileException e) {
      logger.debug(e, "failed to compile {}.{}", targetBehavior.getDeclaringClass().getName(),
          targetBehavior.getName());
    } catch (NotFoundException e) {
      logger.debug(e);
    }
  }

  private static void setBehaviorOrdinals(CtBehavior behavior,
      List<HookSite> hookSites) {
    CtClass ctClass = behavior.getDeclaringClass();
    ClassFile classFile = ctClass.getClassFile();
    ConstPool constPool = classFile.getConstPool();
    Annotation annotation = new Annotation(AppMapAppMethod.class.getName(), constPool);

    MethodEvent[] eventTypes = MethodEvent.values();
    MemberValue[] values = new MemberValue[eventTypes.length];
    for (MethodEvent eventType : eventTypes) {
      IntegerMemberValue v = new IntegerMemberValue(constPool);
      v.setValue(hookSites.get(eventType.getIndex()).getBehaviorOrdinal());
      values[eventType.getIndex()] = v;
    }
    ArrayMemberValue arrayVal = new ArrayMemberValue(constPool);
    arrayVal.setValue(values);
    annotation.addMemberValue("value", arrayVal);
    AnnotationUtil.setAnnotation(new AnnotatedBehavior(behavior), annotation);
  }

  /* Concatenates potentially null strings with no delimeter. The return value
     is guaranteed to be non-null.
  */
  private static String safeConcatStrings(String... strs) {
    return Arrays.stream(strs)
        .filter(Objects::nonNull)
        .collect(Collectors.joining());
  }

  private static String beforeSrcBlock(String... invocations) {
    final String allInvocations = safeConcatStrings(invocations);
    return "{"
        + "com.appland.appmap.process.ThreadLock.current().enter();"
        + allInvocations
        + "}";
  }

  private static String afterSrcBlock(String... invocations) {
    final String allInvocations = safeConcatStrings(invocations);
    return "{"
        + allInvocations
        + "com.appland.appmap.process.ThreadLock.current().exit();"
        + "}";
  }

  private static String catchSrcBlock(String... invocations) {
    final String allInvocations = safeConcatStrings(invocations);
    return "{"
        + allInvocations
        + "com.appland.appmap.process.ThreadLock.current().exit();"
        + "throw $e;"
        + "}";
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

  public CtBehavior getBehavior() {
    return this.hookBehavior;
  }

  public void validate() throws HookValidationException {

  }

  public void validate(CtBehavior behavior) throws HookValidationException {

  }

  public MethodEvent getMethodEvent() {
    return this.sourceSystem.getMethodEvent();
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
        logger.debug(e, "unknown return type");
      }
    }
    return returnType;
  }

  public ISystem getSystem(Class<? extends ISystem> systemClass) {
    for (ISystem system : this.optionalSystems) {
      if (systemClass.isInstance(system)) {
        return system;
      }
    }
    return null;
  }

  public Integer getPosition() {
    return sourceSystem.getHookPosition();
  }
}
