package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

class StatusLine extends ReflectiveType {
  private static String GET_STATUS_CODE = "getStatusCode";

  StatusLine(Object self) {
    super(self);

    addMethods(GET_STATUS_CODE);
  }

  int getStatusCode() {
    return invokeIntMethod(GET_STATUS_CODE);
  }
}