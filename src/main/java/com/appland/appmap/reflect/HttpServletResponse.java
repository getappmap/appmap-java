package com.appland.appmap.reflect;

import java.io.PrintWriter;
import java.io.IOException;
import java.lang.reflect.Method;

public class HttpServletResponse extends ReflectiveType {
  public static final int SC_CONFLICT = 409;
  public static final int SC_NOT_FOUND = 404;
  public static final int SC_OK = 200;

  private static Method fnSetContentType;
  private static Method fnSetContentLength;
  private static Method fnSetStatus;
  private static Method fnGetWriter;
  private static Method fnGetStatus;
  private static Method fnGetContentType;

  public HttpServletResponse(Object self) {
    super(self);

    if (fnSetContentType == null) {
      try {
        fnSetContentType = this.self.getClass().getMethod("setContentType", String.class);
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnSetContentLength == null) {
      try {
        fnSetContentLength = this.self.getClass().getMethod("setContentLength", int.class);
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnSetStatus == null) {
      try {
        fnSetStatus = this.self.getClass().getMethod("setStatus", int.class);
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnGetWriter == null) {
      try {
        fnGetWriter = this.self.getClass().getMethod("getWriter");
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnGetStatus == null) {
      try {
        fnGetStatus = this.self.getClass().getMethod("getStatus");
      } catch (Exception e) {
        /* log an error */
      }
    }

    if (fnGetContentType == null) {
      try {
        fnGetContentType = this.self.getClass().getMethod("getContentType");
      } catch (Exception e) {
        /* log an error */
      }
    }
  }

  public void setContentType(String type) {
    if (fnSetContentType != null) {
      try {
        fnSetContentType.invoke(this.self, type);
      } catch (Exception e) {
        /* log an error */
      }
    }
  }

  public void setContentLength(int len) {
    if (fnSetContentLength != null) {
      try {
        fnSetContentLength.invoke(this.self, len);
      } catch (Exception e) {
        /* log an error */
      }
    }
  }

  public void setStatus(int sc) {
    if (fnSetStatus != null) {
      try {
        fnSetStatus.invoke(this.self, sc);
      } catch (Exception e) {
        /* log an error */
      }
    }
  }

  public PrintWriter getWriter() throws IOException {
    if (fnGetWriter != null) {
      try {
        return (PrintWriter) fnGetWriter.invoke(this.self);
      } catch (Exception e) {
        /* log an error */
      }
    }

    return null;
  }

  public int getStatus() {
    if (fnGetStatus != null) {
      try {
        return (int) fnGetStatus.invoke(this.self);
      } catch (Exception e) {
        /* log an error */
      }
    }

    return -1;
  }

  public String getContentType() {
    if (fnGetContentType != null) {
      try {
        return (String) fnGetContentType.invoke(this.self);
      } catch (Exception e) {
        /* log an error */
      }
    }

    return "";
  }
}