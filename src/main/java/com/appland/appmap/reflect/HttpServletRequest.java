package com.appland.appmap.reflect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class HttpServletRequest extends ReflectiveType {
  private Method fnGetMethod;
  private Method fnGetRequestURI;
  private Method fnGetProtocol;
  private Method fnGetParameterMap;

  public HttpServletRequest(Object self) {
    super(self);

    fnGetMethod = getMethod("getMethod");
    fnGetRequestURI = getMethod("getRequestURI");
    fnGetProtocol = getMethod("getProtocol");
    fnGetParameterMap = getMethod("getParameterMap");
  }

  public String getMethod() {
    return fnGetMethod != null?
      (String)invoke(fnGetMethod)
      : "";
  }

  public String getRequestURI() {
    return fnGetRequestURI != null?
      (String)invoke(fnGetRequestURI)
      : "";
  }

  public String getProtocol() {
    return fnGetProtocol != null?
      (String)invoke(fnGetProtocol)
      : "";
  }

  public Map<String, String[]> getParameterMap() {
    return fnGetProtocol != null?
      (Map<String, String[]>)invoke(fnGetParameterMap)
      : new HashMap<String, String[]>();
  }
}
