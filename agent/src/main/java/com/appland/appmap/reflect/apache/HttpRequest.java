package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

public class HttpRequest extends ReflectiveType {
  private String GET_REQUEST_LINE = "getRequestLine";
  RequestLine rl;

  public HttpRequest(Object self) {
    super(self);
    addMethods(GET_REQUEST_LINE);
    rl = new RequestLine(invokeObjectMethod(GET_REQUEST_LINE));
  }

  public String getMethod() {
    return rl.getMethod();
  }

  public String getUri() {
    return rl.getUri();
  }
}
