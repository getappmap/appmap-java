package com.appland.appmap.output.v1;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import com.appland.appmap.util.FullyQualifiedName;

import javassist.CtBehavior;

/**
 * Represents a snapshot of a method invocation, return, exception or some other
 * kind of runtime event. This class is serialized according to the AppMap
 * schema specification.
 * @see <a href="https://github.com/applandinc/appmap#events">GitHub: AppMap -
 * events</a>
 *
 * NOTE: Fields that are intended to be serialized should conform to the
 * requirements (naming, access level) of fastjson. Fields that are not intended
 * to be serialized should be private. Accessors for these fields should not
 * have "get" and "set" prefixes (since that would cause them to be serialized).
 * See, for example, the "fqn" field.
 */
public class Event {
  private static Integer globalEventId = 0;

  public Integer id;
  public String event;
  public String path;
  public Value receiver;
  public Parameters parameters;

  private FullyQualifiedName fqn;

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

  public ArrayList<ExceptionValue> exceptions;

  @JSONField(name = "http_server_request")
  public HttpServerRequest httpServerRequest;

  @JSONField(name = "http_server_response")
  public HttpServerResponse httpServerResponse;

  @JSONField(name = "http_client_request")
  public HttpClientRequest httpClientRequest;

  @JSONField(name = "http_client_response")
  public HttpClientResponse httpClientResponse;

  public ArrayList<Value> message;

  @JSONField(name = "sql_query")
  public SqlQuery sqlQuery;

  @JSONField(name = "elapsed")
  public Float elapsed = null;

  private boolean frozen = false;
  private boolean ignored = false;
  private String packageName;

  private LocalDateTime startTime;

  private synchronized Integer issueId() {
    return ++globalEventId;
  }

  /**
   * Creates a copy of an existing call event. Does not copy runtime information such as
   * {@link Parameters} or return value.
   *
   * @param master The event to copy information from
   */
  public static Event functionCallEvent(Event master) {
    Event ret = new Event()
        .setEvent("call")
        .setThreadId(Thread.currentThread().getId());
    if (master != null) {
      ret.fqn(master.fqn)
          .setDefinedClass(master.definedClass)
          .setMethodId(master.methodId)
          .setPath(master.path)
          .setLineNumber(master.lineNumber)
          .setStatic(master.isStatic);
    }

    return ret;
  }

  public static Event functionCallEvent() {
    return functionCallEvent(null);
  }

  /**
   * Creates a return event. All the event properties except for the id are
   * left to be filled in later.
   */
  public static Event functionReturnEvent(Event master) {
    // In some cases, such as naming the file output, this information is relied upon.
    // It will be stripped before the event is written.
    // Consider this technical debt...
    Event ret = new Event()
        .setEvent("return");
    if (master != null) {
      ret.fqn(master.fqn)
          .setDefinedClass(master.definedClass)
          .setStatic(master.isStatic)
          .setMethodId(master.methodId);
    }

    return ret;
  }

  public static Event functionReturnEvent() {
    return functionReturnEvent(null);
  }

  /**
   * Constructs a blank event and issues a unique ID.
   */
  public Event() {
    this.setId(issueId());
  }

  /**
   * Creates an event from a CtBehavior.
   * @param behavior Behavior to gather information from
   */
  public Event(CtBehavior behavior) {
    this.fqn(behavior)
        .setDefinedClass(behavior.getDeclaringClass().getName())
        .setMethodId(behavior.getName())
        .setStatic((behavior.getModifiers() & Modifier.STATIC) != 0)
        .setPath(CodeObject.getSourceFilePath(behavior.getDeclaringClass()))
        .setLineNumber(behavior.getMethodInfo().getLineNumber(0))
        .setParameters(new Parameters(behavior));
  }

  private Event setId(Integer id) {
    this.id = id;
    return this;
  }

  /**
   * @return true if the event fields have been frozen. If so, it's too late to change
   * event properties because it's probably already been serialized.
   */
  public boolean frozen() {
    return frozen;
  }

  public void ignore() { ignored = true; }

  /**
   * @return true if the event should not be emitted to the AppMap file.
   */
  public boolean ignored() { return ignored; }

  public boolean hasPackageName() { return packageName != null; }

  public String packageName() { return packageName; }

  public FullyQualifiedName fqn() {
    return this.fqn;
  }

  public Event fqn(CtBehavior behavior) {
    this.fqn = new FullyQualifiedName(behavior);

    return this;
  }

  public Event fqn(FullyQualifiedName fqn) {
    this.fqn = new FullyQualifiedName(fqn);

    return this;
  }

  /**
   * Set the "event" string.
   * @param event "call", "return", etc.
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setEvent(String event) {
    this.event = event;
    return this;
  }

  /**
   * Set the value of "path" for this event.
   * @param path The path of this event
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * Set the value of "defined_class" for this Event.
   * @param definedClass Value of "defined_class"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setDefinedClass(String definedClass) {
    this.definedClass = definedClass;
    String[] tokens = definedClass.split("\\.");
    this.packageName = String.join(".", Arrays.copyOf(tokens, tokens.length - 1));

    return this;
  }

  /**
   * Set the value of "method_id" for this Event.
   * @param methodId Value of "method_id"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setMethodId(String methodId) {
    this.methodId = methodId;
    return this;
  }

  /**
   * Set the value of "lineno" for this Event.
   * @param lineNumber Value of "lineno"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
    return this;
  }

  /**
   * Set the value of "thread_id" for this Event. This is typically obtained via
   * {@code Thread.currentThread().getId()}
   * @param threadId Value of "thread_id"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setThreadId(Long threadId) {
    this.threadId = threadId;
    return this;
  }

  /**
   * Set the value of "parent_id" for this Event.
   * @param parentId Value of "parent_id"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setParentId(Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  /**
   * Set the value of "static" for this Event.
   * @param val Value of "static"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setStatic(Boolean val) {
    this.isStatic = val;
    return this;
  }

  /**
   * Set the value of "return_value" for this Event.
   * @param val Value of "return_value"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setReturnValue(Object val) {
    if (val != null) {
      this.returnValue = new Value(val);
    }

    return this;
  }

  /**
   * Set the value of "exceptions" for this Event.
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes-1">GitHub: AppMap - Common attributes</a>
   */
  public Event setException(Throwable exception) {
    if (exception != null) {
      this.exceptions = new ArrayList<>();
      Throwable t = exception;
      while ( t != null ) {
        exceptions.add(new ExceptionValue(t));
        t = t.getCause();
      }
    }

    return this;
  }

  /**
   * Set the value of "receiver" for this Event.
   * @param val Value of "receiver"
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#function-call-attributes">GitHub: AppMap - Function call attributes</a>
   */
  public Event setReceiver(Object val) {
    if (val != null) {
      this.receiver = new Value(val);
    }

    return this;
  }

  /**
   * Record a parameter value for this event.
   * @param val Value of the parameter
   * @param name Name or identifier of the parameter
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#function-call-attributes">GitHub: AppMap - Function call attributes</a>
   */
  public Event addParameter(Object val, String name) {
    if (this.parameters == null) {
      this.parameters = new Parameters();
    }

    this.parameters.add(new Value(val, name));
    return this;
  }

  /**
   * Record a parameter value for this event.
   * @param val Value object representing the parameter
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#function-call-attributes">GitHub: AppMap - Function call attributes</a>
   */
  public Event addParameter(Value val) {
    if (this.parameters == null) {
      this.parameters = new Parameters();
    }

    this.parameters.add(new Value(val));
    return this;
  }

  /**
   * Set the parameters object of this event.
   * @param parameters A reference to a {@link Parameters} object. This can be null.
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#function-call-attributes">GitHub: AppMap - Function call attributes</a>
   */
  public Event setParameters(Parameters parameters) {
    this.parameters = parameters;
    return this;
  }

  /**
   * Record HTTP server request details for this event.
   * @param method The request method
   * @param path The request URI path
   * @param protocol The request protocol
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-request-attributes">GitHub: AppMap - HTTP server request attributes</a>
   */
  public Event setHttpServerRequest(String method, String path, String protocol, Map<String, String> headers) {
    clearFunctionFields();
    this.httpServerRequest = new HttpServerRequest()
        .setMethod(method)
        .setPath(path)
        .setProtocol(protocol)
        .setHeaders(headers);
    return this;
  }

  /**
   * Record HTTP server response details for this event.
   * @param status The response status returned
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-response-attributes">GitHub: AppMap - HTTP server response attributes</a>
   */
  public Event setHttpServerResponse(Integer status) {
    this.httpServerResponse = new HttpServerResponse()
        .setStatus(status);
    return this;
  }

  /**
   * Record HTTP server response details for this event.
   * @param status The response status returned
   * @param mimeType The MIME type of the response ({@code Content-Type} header)
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-response-attributes">GitHub: AppMap - HTTP server response attributes</a>
   */
  public Event setHttpServerResponse(Integer status, Map<String, String> headers) {
    this.httpServerResponse = new HttpServerResponse()
        .setStatus(status)
        .setHeaders(headers);
    return this;
  }

  /**
   * Record HTTP client request details for this event.
   * 
   * @param method   The request method
   * @param url      The request URI
   * @param protocol The request protocol
   * @return {@code this}
   * @see <a href=
   *      "https://github.com/applandinc/appmap#http-client-request-attributes">GitHub:
   *      AppMap - HTTP client request attributes</a>
   */
  public Event setHttpClientRequest(String method, String url) {
    clearFunctionFields();
    this.httpClientRequest = new HttpClientRequest()
        .setMethod(method)
        .setURL(url);
    return this;
  }

  /**
   * Record HTTP client response details for this event.
   * @param status The response status returned
   * @param mimeType The MIME type of the response ({@code Content-Type} header)
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-client-response-attributes">GitHub: AppMap - HTTP client response attributes</a>
   */
  public Event setHttpClientResponse(Integer status, String mimeType) {
    this.httpClientResponse = new HttpClientResponse()
            .setStatus(status)
            .setMimeType(mimeType);
    return this;
  }

  /**
   * Record a parameter of an HTTP server requests. This should be called once for each parameter.
   * @param val A {@link Value} representing the request parameter.
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#message-attributes">GitHub: AppMap - Message attributes</a>
   */
  public Event addMessageParam(Value val) {
    if (this.message == null) {
      this.message = new ArrayList<>();
    }

    this.message.add(val);
    return this;
  }

  /**
   * Record a parameter of an HTTP server requests. This should be called once for each parameter.
   * @param name The name of the parameter to be recorded
   * @param val The value of the parameter to be recorded
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#message-attributes">GitHub: AppMap - Message attributes</a>
   */
  public Event addMessageParam(String name, Object val) {
    Value valObject = new Value(val, name);
    this.addMessageParam(valObject);
    return this;
  }

  /**
   * Record a SQL query.
   * @param sql The SQL query to be recorded
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#sql-query-attributes">GitHub: AppMap - SQL query attributes</a>
   */
  public Event setSqlQuery(String databaseType, String sql) {
    clearFunctionFields();
    this.sqlQuery = new SqlQuery(databaseType, sql);
    return this;
  }

  public Event setStartTime() {
    this.startTime = LocalDateTime.now();
    return this;
  }

  public Event measureElapsed(Event callEvent) {
    LocalDateTime endTime = LocalDateTime.now();
    double duration = Duration.between(callEvent.startTime, endTime).toNanos() / 10e09;
    this.elapsed = Float.valueOf((float)duration);
    return this;
  }

  /**
   * Convert all referenced Objects to Strings. This mitigates the risk of null pointer exceptions
   * or reading otherwise invalid state when later serializing this event. This should always be
   * called once an event has been finalized.
   */
  public Event freeze() {
    if ( this.frozen ) {
      return this;
    }

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

    if (this.exceptions != null) {
      for (ExceptionValue e : this.exceptions) {
        e.freeze();
      }
    }

    this.frozen = true;
    return this;
  }

  public void defrost() {
    this.frozen = false;
  }
  
  void clearFunctionFields() {
    methodId = null;
    definedClass = null;
    path = null;
    lineNumber = null;
    isStatic = null;
  }
}
