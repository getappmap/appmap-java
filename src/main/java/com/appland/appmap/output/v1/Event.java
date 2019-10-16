package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Event {
  private static Integer globalEventId = 0;

  public Integer id;
  public String event;
  public String path;
  public Value receiver;
  public ArrayList<Value> parameters = new ArrayList<Value>();

  @JSONField(name = "defined_class")
  public String definedClass;

  @JSONField(name = "method_id")
  public String methodId;

  @JSONField(name = "lineno")
  public Integer lineNumber;

  @JSONField(name = "thread_id")
  public Long threadId;

  @JSONField(name = "parent_id")
  public Integer parentId;

  @JSONField(name = "static")
  public Boolean isStatic;

  @JSONField(name = "return_value")
  public Value returnValue;

  public Event() {

  }

  public Event(Event master) {
    this.setMethodId(master.methodId)
        .setDefinedClass(master.definedClass)
        .setPath(master.path)
        .setLineNumber(master.lineNumber)
        .setStatic(master.isStatic);
  }

  public Event(Method method, String eventType) {
    this.setId(globalEventId++)
        .setMethodId(method.getName())
        .setDefinedClass(method.getDeclaringClass().getName())
        .setEvent(eventType)
        .setThreadId(Thread.currentThread().getId());
  }

  public Event setId(Integer id) {
    this.id = id;
    return this;
  }

  public Event setEvent(String event) {
    this.event = event;
    return this;
  }

  public Event setPath(String path) {
    this.path = path;
    return this;
  }

  public Event setDefinedClass(String definedClass) {
    this.definedClass = definedClass;
    return this;
  }

  public Event setMethodId(String methodId) {
    this.methodId = methodId;
    return this;
  }

  public Event setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
    return this;
  }

  public Event setThreadId(Long threadId) {
    this.threadId = threadId;
    return this;
  }

  public Event setParentId(Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  public Event setStatic(Boolean val) {
    this.isStatic = val;
    return this;
  }

  public Event setReturnValue(Object val) {
    if (val != null) {
      this.returnValue = new Value(val);
    }

    return this;
  }

  public Event setReceiver(Object val) {
    if (val != null) {
      this.receiver = new Value(val);
    }

    return this;
  }

  public Event addParameter(Object val, String name) {
    if (val == null) {
      return this;
    }

    if (this.parameters == null) {
      this.parameters = new ArrayList<Value>();
    }

    this.parameters.add(new Value(val, name));

    return this;
  }

  public Event addParameter(Value val) {
    if (val == null) {
      return this;
    }

    if (this.parameters == null) {
      this.parameters = new ArrayList<Value>();
    }

    this.parameters.add(new Value(val));

    return this;
  }

  public Event setParameters(ArrayList<Value> parameters) {
    this.parameters = parameters;
    return this;
  }
}