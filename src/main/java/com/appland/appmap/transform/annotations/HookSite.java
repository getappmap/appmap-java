package com.appland.appmap.transform.annotations;

import com.appland.appmap.output.v1.Parameters;

import java.io.StringWriter;
import java.util.stream.Collectors;

import javassist.CtBehavior;

/**
 * Represents a hook to be applied to a behavior.
 */
public class HookSite {
  private final Hook hook;
  private final String hookInvocation;
  private final MethodEvent methodEvent;
  private final Boolean ignoresGlobalLock;

  HookSite(Hook hook, Integer behaviorOrdinal, Parameters parameters) {
    this.methodEvent = hook.getMethodEvent();
    this.hook = hook;
    this.ignoresGlobalLock = (Boolean) AnnotationUtil.getValue(
      hook.getBehavior(),
      ContinueHooking.class,
      false);

    final String event = String.format("%s.get().cloneEventTemplate(%d, \"%s\")",
        "com.appland.appmap.record.EventTemplateRegistry",
        behaviorOrdinal,
        this.methodEvent.getEventString());

    final String args = parameters
        .stream()
        .map(param -> {
          return (param.classType == null || param.classType.isEmpty())
              ? param.name
              : String.format(("(%s) %s"), param.classType, param.name);
        })
        .collect(Collectors.joining(", "))
        .replace(SourceMethodSystem.EVENT_TOKEN, event);

    String invocation = hook.getSourceSystem().toString() + "(" + args + ");";

    if (!this.getUniqueKey().isEmpty()) {
      invocation = "if (com.appland.appmap.process.ThreadLock.current().hasUniqueLock(\""
          + this.getUniqueKey()
          + "\")) {"
          + invocation
          + "}";
    }

    if (this.ignoresGlobalLock()) {
      this.hookInvocation = invocation;
    } else {
      this.hookInvocation = "if (com.appland.appmap.process.ThreadLock.current().lock()) {"
        + invocation
        + "com.appland.appmap.process.ThreadLock.current().unlock();"
        + "}";
    }
  }

  public String getHookInvocation() {
    return this.hookInvocation;
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
}
