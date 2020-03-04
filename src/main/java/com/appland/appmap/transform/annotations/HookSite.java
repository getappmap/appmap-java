package com.appland.appmap.transform.annotations;

import java.io.StringWriter;
import java.util.stream.Collectors;

import com.appland.appmap.output.v1.Parameters;

import javassist.CtBehavior;

public class HookSite {
  private final Hook hook;
  private final String hookInvocation;
  private final MethodEvent methodEvent;

  HookSite(Hook hook, Integer behaviorOrdinal, Parameters parameters) {
    this.methodEvent = hook.getMethodEvent();

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

    this.hook = hook;
    this.hookInvocation = hook.getSourceSystem().toString() + "(" + args + ");";
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
}