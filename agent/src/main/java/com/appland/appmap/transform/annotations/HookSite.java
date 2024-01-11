package com.appland.appmap.transform.annotations;

import java.util.stream.Collectors;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Parameters;

/**
 * An example of hook, applied to a behavior. Holds metadata which can be eventually used to transform the behavior
 * bytecode to record the AppMap event.
 */
public class HookSite {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private final Hook hook;
  private final Integer behaviorOrdinal;
  private String hookInvocation;
  private final MethodEvent methodEvent;
  private Boolean ignoresGlobalLock;
  private final HookBinding binding;

  /**
   * @param behaviorOrdinal Used to obtain a template for the event from the event template registry.
   * @param parameters Parameters that will be reported in the AppMap.
   * @see com.appland.appmap.record.EventTemplateRegistry
   */
  HookSite(Hook hook, Integer behaviorOrdinal, HookBinding binding) {
    this.methodEvent = hook.getMethodEvent();
    this.hook = hook;
    this.behaviorOrdinal = behaviorOrdinal;
    this.binding = binding;
    ignoresGlobalLock = (Boolean)AnnotationUtil.getValue(
      hook.getBehavior(),
      ContinueHooking.class,
        false);
  }

  private void initHookInvocation() {
    logger.trace("hook: {}", () -> hook.getBehavior().getLongName());

    final String event;
    if ( methodEvent.getEventString().equals("call") ) {
      event = String.format("%s.get().buildCallEvent(%d)",
          "com.appland.appmap.record.EventTemplateRegistry",
          behaviorOrdinal);
    } else {
      event = String.format("%s.get().buildReturnEvent(%d)",
          "com.appland.appmap.record.EventTemplateRegistry",
          behaviorOrdinal);
    }

    Parameters parameters = hook.getRuntimeParameters(binding);
    final String args = parameters
        .stream()
        .map(param -> (param.classType == null || param.classType.isEmpty())
            ? param.name
            : String.format(("(%s) %s"), param.classType, param.name))
        .collect(Collectors.joining(", "))
        .replace(SourceMethodSystem.EVENT_TOKEN, event);

    String invocation = hook.getSourceSystem().toString() + "(" + args + ");";

    if (!getUniqueKey().isEmpty()) {
      invocation = "if (com.appland.appmap.process.ThreadLock.current().hasUniqueLock(\""
          + getUniqueKey()
          + "\")) {"
          + invocation
          + "}";
    }

    if (ignoresGlobalLock()) {
      hookInvocation = invocation;
    } else {
      hookInvocation = "if (com.appland.appmap.process.ThreadLock.current().lock()) {"
          + invocation
          + "com.appland.appmap.process.ThreadLock.current().unlock();"
          + "}";
    }
  }

  public String getHookInvocation() {
    if (hookInvocation == null) {
      initHookInvocation();
    }

    return hookInvocation;
  }

  public MethodEvent getMethodEvent() {
    return this.methodEvent;
  }

  public String getUniqueKey() {
    return hook.getUniqueKey();
  }

  public Hook getHook() {
    return this.hook;
  }

  public Boolean ignoresGlobalLock() {
    return this.ignoresGlobalLock;
  }

  public Integer getBehaviorOrdinal() {
    return behaviorOrdinal;
  }
}
