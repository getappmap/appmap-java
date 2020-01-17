package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Event {
  private static Integer globalEventId = 0;
  private Boolean alive = true;

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

  @JSONField(name = "http_server_request")
  public HttpServerRequest httpRequest;

  @JSONField(name = "http_server_response")
  public HttpServerResponse httpResponse;

  @JSONField(name = "message")
  public ArrayList<Value> message;

  @JSONField(name = "sql_query")
  public SqlQuery sqlQuery;

  private synchronized Integer issueId() {
    return ++globalEventId;
  }

  public Event() {
    this.setId(issueId());
  }

  public Event(Event master) {
    this.setId(issueId())
        .setMethodId(master.methodId)
        .setDefinedClass(master.definedClass)
        .setPath(master.path)
        .setLineNumber(master.lineNumber)
        .setStatic(master.isStatic);
  }

  public Event(Method method, String eventType) {
    this.setId(issueId())
        .setMethodId(method.getName())
        .setDefinedClass(method.getDeclaringClass().getName())
        .setEvent(eventType)
        .setThreadId(Thread.currentThread().getId());
  }

  private Event setId(Integer id) {
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

  public Value getParameter(String name) {
    if (this.parameters == null) {
      return null;
    }

    for (Value param : this.parameters) {
      if (param.name.equals(name)) {
        return param;
      }
    }

    return null;
  }

  public Value getParameter(Integer index) {
    if (this.parameters == null) {
      return null;
    }

    try {
      return this.parameters.get(index);
    } catch (IndexOutOfBoundsException e) {
      System.err.printf("failed to get param at index %d: %s\n",
          index,
          e.getMessage());
    }

    return null;
  }

  public Value popParameter(String name) {
    if (this.parameters == null) {
      return null;
    }

    final int numParams = this.parameters.size();
    for (int i = 0; i < numParams; ++i) {
      final Value param = this.parameters.get(i);

      if (param.name.equals(name)) {
        this.parameters.remove(i);
        return param;
      }
    }

    return null;
  }

  public Event setHttpServerRequest(String method, String path, String protocol) {
    this.httpRequest = new HttpServerRequest()
        .setMethod(method)
        .setPath(path)
        .setProtocol(protocol);
    return this;
  }

  public Event setHttpServerResponse(Integer status) {
    this.httpResponse = new HttpServerResponse()
        .setStatus(status);
    return this;
  }

  public Event addMessageParam(Value val) {
    if (this.message == null) {
      this.message = new ArrayList<Value>();
    }

    this.message.add(val);
    return this;
  }

  public Event addMessageParam(String name, Object val) {
    Value valObject = new Value(val, name);
    this.addMessageParam(valObject);
    return this;
  }

  public Event setSqlQuery(String sql) {
    this.sqlQuery = new SqlQuery().setSql(sql);
    return this;
  }

  public Event freeze() {
    if (this.parameters != null) {
      for (Value value : this.parameters) {
        value.freeze();
      }
    }

    if (this.receiver != null) {
      this.receiver.freeze();
    }

    if (this.returnValue != null) {
      this.returnValue.freeze();
    }

    return this;
  }

  public Event kill() {
    this.alive = false;
    return this;
  }

  public Boolean isAlive() {
    return this.alive;
  }
}