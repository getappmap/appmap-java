package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

class Header extends ReflectiveType {
  private static String GET_NAME = "getName";
  private static String GET_VALUE = "getValue";

  Header(Object self) {
    super(self);

    addMethods(GET_NAME, GET_VALUE);
  }

  String getName() {
    return invokeStringMethod(GET_NAME);
  }

  String getValue() {
    return invokeStringMethod(GET_VALUE);
  }
}