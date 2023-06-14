package com.appland.appmap.process.hooks.http;

import com.appland.appmap.reflect.ReflectiveType;

class ServletConfig extends ReflectiveType {
  private static String GET_CONTEXT = "getServletContext";

  ServletConfig(Object self) {
    super(self);

    addMethods(GET_CONTEXT);
  }

  ServletContext getContext() {
    return new ServletContext(invokeObjectMethod(GET_CONTEXT));
  }

}