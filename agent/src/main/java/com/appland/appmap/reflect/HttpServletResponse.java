package com.appland.appmap.reflect;

import java.io.PrintWriter;
import java.lang.reflect.Method;

public class HttpServletResponse extends HttpHeaders {
  public static final int SC_CONFLICT = 409;
  public static final int SC_NOT_FOUND = 404;
  public static final int SC_OK = 200;

  private final Method fnSetContentType;
  private final Method fnSetContentLength;
  private final Method fnSetStatus;
  private final Method fnGetWriter;
  private final Method fnGetStatus;
  private final Method fnGetContentType;

  public HttpServletResponse(Object self) {
    super(self);

    fnSetContentType = getMethod("setContentType", String.class);
    fnSetContentLength = getMethod("setContentLength", int.class);
    fnSetStatus = getMethod("setStatus", int.class);
    fnGetWriter = getMethod("getWriter");
    fnGetStatus = getMethod("getStatus");
    fnGetContentType = getMethod("getContentType");
  }

  public void setContentType(String type) {
    if (fnSetContentType != null) {
      invokeWrappedMethod(fnSetContentType, type);
    }
  }

  public void setContentLength(int len) {
    if (fnSetContentLength != null) {
      invokeWrappedMethod(fnSetContentLength, len);
    }
  }

  public void setStatus(int sc) {
    if (fnSetStatus != null) {
      invokeWrappedMethod(fnSetStatus, sc);
    }
  }

  public PrintWriter getWriter() {
    return fnGetWriter != null?
      (PrintWriter) invokeWrappedMethod(fnGetWriter)
      : null;
  }

  public int getStatus() {
    return fnGetStatus != null?
      (int) invokeWrappedMethod(fnGetStatus)
      : -1;
  }

  public String getContentType() {
    return fnGetContentType != null?
      (String) invokeWrappedMethod(fnGetContentType)
      : "";
  }
}
