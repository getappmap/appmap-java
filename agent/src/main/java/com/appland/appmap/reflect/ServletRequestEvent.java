package com.appland.appmap.reflect;

public class ServletRequestEvent extends ReflectiveType {
  private static String GET_SERVLET_REQUEST = "getServletRequest";

  public ServletRequestEvent(Object self) {
    super(self);

    addMethods(GET_SERVLET_REQUEST);
  }

  public HttpServletRequest getServletRequest() {
    return new HttpServletRequest(invokeObjectMethod(GET_SERVLET_REQUEST));
  }
}