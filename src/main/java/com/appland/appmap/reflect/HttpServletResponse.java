package com.appland.appmap.reflect;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

public class HttpServletResponse extends ReflectiveType {
  public static final int SC_CONFLICT = 409;
  public static final int SC_NOT_FOUND = 404;
  public static final int SC_OK = 200;

  private Method fnSetContentType;
  private Method fnSetContentLength;
  private Method fnSetStatus;
  private Method fnGetWriter;
  private Method fnGetStatus;
  private Method fnGetContentType;

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
      invoke(fnSetContentType, type);
    }
  }

  public void setContentLength(int len) {
    if (fnSetContentLength != null) {
      invoke(fnSetContentLength, len);
    }
  }

  public void setStatus(int sc) {
    if (fnSetStatus != null) {
      invoke(fnSetStatus, sc);
    }
  }

  public PrintWriter getWriter() throws IOException {
    return fnGetWriter != null?
      (PrintWriter)invoke(fnGetWriter)
      : null;
  }

  public int getStatus() {
    return fnGetStatus != null?
      (int)invoke(fnGetStatus)
      : -1;
  }

  public String getContentType() {
    return fnGetContentType != null?
      (String)invoke(fnGetContentType)
      : "";
  }
}
