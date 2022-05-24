package com.appland.appmap.reflect;

import java.lang.reflect.Method;

class HttpHeaderDelegate extends ReflectiveType {
  final Method fnGetHeader;
  final Method fnGetHeaderNames;

  public HttpHeaderDelegate(Object self) {
    super(self);

    fnGetHeader = getMethod("getHeader", String.class);
    fnGetHeaderNames = getMethod("getHeaderNames");
  }
}