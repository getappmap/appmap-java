package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

class HttpEntity extends ReflectiveType {
  private static String GET_CONTENT_TYPE = "getContentType";

  HttpEntity(Object self) {
    super(self);

    addMethods(GET_CONTENT_TYPE);
  }

  String getContentType() {
    return new Header(invokeObjectMethod(GET_CONTENT_TYPE)).getValue();
  }
}