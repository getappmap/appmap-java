package com.appland.appmap.process.hooks.http;

import java.util.EnumSet;
import java.util.EventListener;

import com.appland.appmap.reflect.ReflectiveType;

public class ServletContext extends ReflectiveType {
  private static String GET_SERVLET_CONTEXT_NAME = "getServletContextName";
  private static String ADD_LISTENER = "addListener";
  private static String ADD_FILTER = "addFilter";
  private static String GET_ATTRIBUTE = "getAttribute";
  private static String SET_ATTRIBUTE = "setAttribute";
  private static String GET_CONTEXT_PATH = "getContextPath";

  public static class FilterRegistration extends ReflectiveType {
    private static String ADD_MAPPING_FOR_URL_PATTERNS = "addMappingForUrlPatterns";

    public FilterRegistration(Object self) {
      super(self);
      addMethod(ADD_MAPPING_FOR_URL_PATTERNS, EnumSet.class, Boolean.TYPE, String[].class);
    }

    public void addMappingForUrlPatterns(EnumSet<?> types, boolean isMatchAfter, String... patterns) {
      invokeVoidMethod(ADD_MAPPING_FOR_URL_PATTERNS, types, isMatchAfter, patterns);
    }
  }

  public ServletContext(Object self) {
    super(self);

    addMethods(GET_SERVLET_CONTEXT_NAME);
    addMethod(ADD_LISTENER, EventListener.class);
    if (!addMethod(ADD_FILTER, "java.lang.String", "javax.servlet.Filter")
        && !addMethod(ADD_FILTER, "java.lang.String", "jakarta.servlet.Filter")) {
      throw new InternalError("no addFilter method");
    }
    addMethod(GET_ATTRIBUTE, String.class);
    addMethod(SET_ATTRIBUTE, String.class, Object.class);
    addMethods(GET_CONTEXT_PATH);
  }

  public String getServletContextName() {
    return invokeStringMethod(GET_SERVLET_CONTEXT_NAME);
  }

  public String getContextPath() {
    return invokeStringMethod(GET_CONTEXT_PATH);
  }

  public void addListener(Object listener) {
    invokeVoidMethod(ADD_LISTENER, listener);
  }

  public FilterRegistration addFilter(Object filterName, Object filter) {
    return new FilterRegistration(invokeObjectMethod(ADD_FILTER, filterName, filter));
  }

  public Object getAttribute(String name) {
    return invokeObjectMethod(GET_ATTRIBUTE, name);
  }

  public void setAttribute(String name, Object value) {
    invokeVoidMethod(SET_ATTRIBUTE, name, value);
  }

}