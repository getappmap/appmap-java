package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

class RequestLine extends ReflectiveType {
  private static String GET_METHOD = "getMethod";
  private static String GET_URI = "getUri";

  RequestLine(Object self) {
    super(self);

    addMethods(GET_METHOD, GET_URI);
  }

  String getMethod() {
    return invokeStringMethod(GET_METHOD);
  }

  String getUri() {
    return invokeStringMethod(GET_URI);
  }
}