package com.appland.appmap.reflect;

import java.lang.reflect.Method;

class HttpMessageDelegate extends ReflectiveType {
  final Method fnGetHeader;
  final Method fnGetHeaderNames;

  public HttpMessageDelegate(Object self) {
    super(self);

    fnGetHeader = getMethod("getHeader", String.class);
    fnGetHeaderNames = getMethod("getHeaderNames");
  }
}