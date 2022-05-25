package com.appland.appmap.reflect;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HttpServletRequest extends ReflectiveType implements HttpHeaders {
  private final HttpHeaderDelegate headerDelegate;

  private final Method fnGetMethod;
  private final Method fnGetRequestURI;
  private final Method fnGetProtocol;
  private final Method fnGetParameterMap;
  private final Method fnGetAttribute;
  private final Method fnSetAttribute;
  private final Method fnGetAttributeNames;

  public HttpServletRequest(Object self) {
    super(self);
    this.headerDelegate = new HttpHeaderDelegate(self);
    
    fnGetMethod = getMethod("getMethod");
    fnGetRequestURI = getMethod("getRequestURI");
    fnGetProtocol = getMethod("getProtocol");
    fnGetParameterMap = getMethod("getParameterMap");
    fnGetAttribute = getMethod("getAttribute", String.class);
    fnSetAttribute = getMethod("setAttribute", String.class, Object.class);
    fnGetAttributeNames = getMethod("getAttributeNames");
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

  public Object getAttribute(String name) {
    return fnGetAttribute != null?
      invokeWrappedMethod(fnGetAttribute, name)
      : null;
  }

  public void setAttribute(String key, Object value) {
    if (fnSetAttribute != null) {
      invokeWrappedMethod(fnSetAttribute, key, value);
    }
  }
  
  @SuppressWarnings("unchecked")
  public Enumeration<String> getAttributeNames() {
    return fnGetAttributeNames != null?
      (Enumeration<String>) invokeWrappedMethod(fnGetAttributeNames)
      : null;
  }

  @Override
  public HttpHeaderDelegate getHeaderDelegate() {
    return headerDelegate;
  }
}
