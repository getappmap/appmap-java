package com.appland.appmap.reflect;

import java.lang.reflect.Method;
import java.util.Vector;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HttpHeaders extends ReflectiveType {
  protected final Method fnGetHeader;
  protected final Method fnGetHeaderNames;

  public HttpHeaders(Object self) {
    super(self);

    fnGetHeader = getMethod("getHeader", String.class);
    fnGetHeaderNames = getMethod("getHeaderNames");
  }

  public String getHeader(String name) {
    return fnGetHeader != null?
      (String) invokeWrappedMethod(fnGetHeader, name)
      : "";
  }

  @SuppressWarnings("unchecked")
  public Enumeration<String> getHeaderNames() {
    try {
      return (Enumeration<String>) invokeWrappedMethod(fnGetHeaderNames);
    } catch (ClassCastException _e) {
      return Collections.enumeration((Collection<String>) invokeWrappedMethod(fnGetHeaderNames));
    } catch (Exception e) {
      // Fall through
    }

    return Collections.enumeration(Collections.emptyList());
  }

  /**
   * Non-standard utility method. Retrieves all headers.
   */
  public Map<String, String> getHeaders() {
    HashMap<String, String> headers = new HashMap<>();

    for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
      String headerName = (String) e.nextElement();
      headers.put(headerName, this.getHeader(headerName));
    }

    return headers;
  }
}