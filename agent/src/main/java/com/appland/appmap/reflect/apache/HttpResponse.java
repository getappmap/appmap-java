package com.appland.appmap.reflect.apache;

import com.appland.appmap.reflect.ReflectiveType;

public class HttpResponse extends ReflectiveType {
  private static String GET_STATUS_LINE = "getStatusLine";
  private static String GET_ENTITY = "getEntity";

  public HttpResponse(Object self) {
    super(self);
    addMethods(GET_STATUS_LINE, GET_ENTITY);
  }

  public int getStatusCode() {
    StatusLine sl = new StatusLine(invokeObjectMethod(GET_STATUS_LINE));
    return sl.getStatusCode();
  }

  public String getContentType() {
    HttpEntity he = new HttpEntity(invokeObjectMethod(GET_ENTITY));

    return he.getContentType();
  }
}
