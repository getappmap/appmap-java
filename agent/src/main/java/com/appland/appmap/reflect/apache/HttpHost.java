package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

public class HttpHost extends ReflectiveType {
  private String TO_URI = "toURI";

  public HttpHost(Object self) {
    super(self);
    addMethods(TO_URI);
  }

  public String toURI() {
    return invokeStringMethod(TO_URI);
  }
}
