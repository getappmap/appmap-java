package com.appland.appmap.reflect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

public class HttpServletRequest extends ReflectiveType {
  private static Method fnGetMethod;
  private static Method fnGetRequestURI;
  private static Method fnGetProtocol;
  private static Method fnGetParameterMap;

  public HttpServletRequest(Object self) {
    super(self);

    if (fnGetMethod == null) {
      try {
        fnGetMethod = this.self.getClass().getMethod("getMethod");
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnGetRequestURI == null) {
      try {
        fnGetRequestURI = this.self.getClass().getMethod("getRequestURI");
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnGetProtocol == null) {
      try {
        fnGetProtocol = this.self.getClass().getMethod("getProtocol");
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnGetParameterMap == null) {
      try {
        fnGetParameterMap = this.self.getClass().getMethod("getParameterMap");
      } catch (Exception e) {
        /* log an error */
      }
    }
  }

  public String getMethod() {
    if (fnGetMethod != null) {
      try {
        return (String) fnGetMethod.invoke(this.self);
      } catch (Exception e) {
        /* log an error */
      }
    }

    return "";
  }

  public String getRequestURI() {
    if (fnGetRequestURI != null) {
      try {
        return (String) fnGetRequestURI.invoke(this.self);
      } catch (Exception e) {
        /* log an error */
      }
    }

    return "";
  }

  public String getProtocol() {
    if (fnGetProtocol != null) {
      try {
        return (String) fnGetProtocol.invoke(this.self);
      } catch (Exception e) {
        /* log an error */
      }
    }

    return "";
  }

  public Map<String, String[]> getParameterMap() {
    if (fnGetProtocol != null) {
      try {
        return (Map<String, String[]>) fnGetParameterMap.invoke(this.self);
      } catch (Exception e) {
        /* log an error */
      }
    }

    return new HashMap<String, String[]>();
  }
}