package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

  @JSONField(name = "http_server_request")
  public HttpServerRequest httpRequest;

  @JSONField(name = "http_server_response")
  public HttpServerResponse httpResponse;

  @JSONField(name = "message")
  public Map<String, String> message;

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

  public Event addMessageParam(String key, String val) {
    if (this.message == null) {
      this.message = new HashMap<>();
    }

    this.message.put(key, val);
    return this;
  }
}