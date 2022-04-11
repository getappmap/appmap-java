package com.appland.appmap.reflect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class HttpServletRequest extends HttpHeaders {
  private final Method fnGetMethod;
  private final Method fnGetRequestURI;
  private final Method fnGetProtocol;
  private final Method fnGetParameterMap;

  public HttpServletRequest(Object self) {
    super(self);

    fnGetMethod = getMethod("getMethod");
    fnGetRequestURI = getMethod("getRequestURI");
    fnGetProtocol = getMethod("getProtocol");
    fnGetParameterMap = getMethod("getParameterMap");
  }

  public String getMethod() {
    return fnGetMethod != null?
      (String) invokeWrappedMethod(fnGetMethod)
      : "";
  }

  public String getRequestURI() {
    return fnGetRequestURI != null?
      (String) invokeWrappedMethod(fnGetRequestURI)
      : "";
  }

  public String getProtocol() {
    return fnGetProtocol != null?
      (String) invokeWrappedMethod(fnGetProtocol)
      : "";
  }

  @SuppressWarnings("unchecked")
  public Map<String, String[]> getParameterMap() {
    return fnGetProtocol != null?
      (Map<String, String[]>) invokeWrappedMethod(fnGetParameterMap)
      : new HashMap<String, String[]>();
  }
}
