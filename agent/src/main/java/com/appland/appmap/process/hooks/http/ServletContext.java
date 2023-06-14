package com.appland.appmap.process.hooks.http;

import java.util.EventListener;

import com.appland.appmap.reflect.ReflectiveType;

public class ServletContext extends ReflectiveType {
  private static String GET_SERVLET_CONTEXT_NAME = "getServletContextName";
  private static String ADD_LISTENER = "addListener";
  private static String GET_ATTRIBUTE = "getAttribute";
  private static String SET_ATTRIBUTE = "setAttribute";

  public ServletContext(Object self) {
    super(self);

    addMethods(GET_SERVLET_CONTEXT_NAME);
    addMethod(ADD_LISTENER, EventListener.class);
    addMethod(GET_ATTRIBUTE, String.class);
    addMethod(SET_ATTRIBUTE, String.class, Object.class);
  }

  public String getServletContextName() {
    return invokeStringMethod(GET_SERVLET_CONTEXT_NAME);
  }

  public void addListener(Object listener) {
    invokeVoidMethod(ADD_LISTENER, listener);
  }

  public Object getAttribute(String name) {
    return invokeObjectMethod(GET_ATTRIBUTE, name);
  }

  public void setAttribute(String name, Object value) {
    invokeVoidMethod(SET_ATTRIBUTE, name, value);
  }

}