package com.appland.appmap.reflect.apache;

import java.net.URI;

import com.appland.appmap.reflect.ReflectiveType;

public class HttpUriRequest extends ReflectiveType {
  private String GET_METHOD = "getMethod";
  private String GET_URI = "getURI";

  public HttpUriRequest(Object self) {
    super(self);
    addMethods(GET_METHOD, GET_URI);
  }

  public String getMethod() {
    return invokeStringMethod(GET_METHOD);
  }

  public URI getURI() {
    return (URI)invokeObjectMethod(GET_URI);
  }
}
