package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

public class NameValuePair extends ReflectiveType {
  private static String GET_NAME = "getName";
  private static String GET_VALUE = "getValue";

  public NameValuePair(Object self) {
    super(self);
    addMethods(GET_NAME, GET_VALUE);
  }

  public String getName() {
    return invokeStringMethod(GET_NAME);
  }

  public String getValue() {
    return invokeStringMethod(GET_VALUE);
  }
}