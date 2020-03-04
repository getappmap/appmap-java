package com.appland.appmap.transform.annotations;

import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.EventTemplateRegistry;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

class HookSourceCode {
  private final static String PKG_PROCESS = "com.appland.appmap.process";
  private final static String PKG_TRANSFORM = "com.appland.appmap.transform";
  private final static String EVENT_TEMPLATE_REGISTRY = "com.appland.appmap.record.EventTemplateRegistry";

  private final StringWriter writer = new StringWriter();
  private Hook hook;
  private Integer behaviorOrdinal;
  private CtBehavior behavior;
  private Parameters parameters;
  private MethodEvent methodEvent;
  private SourceMethodSystem sourceSystem;

  public HookSourceCode( Hook hook,
                                CtBehavior behavior,
                                Integer behaviorOrdinal,
                                MethodEvent methodEvent,
                                Parameters parameters) {
    this.hook = hook;
    this.behavior = behavior;
    this.behaviorOrdinal = behaviorOrdinal;
    this.parameters = parameters;
    this.methodEvent = this.hook.getMethodEvent();
    this.sourceSystem = this.hook.getSourceSystem();
  }

  private HookSourceCode writeTry() {
    this.writer.write("try {");
    return this;
  }

  private HookSourceCode beginBlock() {
    this.writer.write("{");
    return this;
  }

  private HookSourceCode endBlock() {
    this.writer.write("}");
    return this;
  }

  private HookSourceCode writeHookInvocation() {
    final String event = String.format("%s.get().cloneEventTemplate(%d, \"%s\")",
        EVENT_TEMPLATE_REGISTRY,
        behaviorOrdinal,
        this.methodEvent.getEventString());

    final String args = this.parameters
        .stream()
        .map(param -> {
          return (param.classType == null || param.classType.isEmpty())
              ? param.name
              : String.format(("(%s) %s"), param.classType, param.name);
        })
        .collect(Collectors.joining(", "))
        .replace(SourceMethodSystem.EVENT_TOKEN, event);

    this.writer.write(this.sourceSystem.toString());
    this.writer.write("(");
    this.writer.write(args);
    this.writer.write(");");

    return this;
  }
}