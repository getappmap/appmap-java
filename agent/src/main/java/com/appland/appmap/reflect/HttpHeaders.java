package com.appland.appmap.reflect;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public interface HttpHeaders {
  HttpHeaderDelegate getHeaderDelegate();

  default String getHeader(String name) {
    HttpHeaderDelegate delegate = getHeaderDelegate();
    return delegate.fnGetHeader != null?
    (String) delegate.invokeWrappedMethod(delegate.fnGetHeader, name)
    : "";
  }
  
  @SuppressWarnings("unchecked")
  default Enumeration<String> getHeaderNames() {
    HttpHeaderDelegate delegate = getHeaderDelegate();
    try {
      return (Enumeration<String>) delegate.invokeWrappedMethod(delegate.fnGetHeaderNames);
    } catch (ClassCastException _e) {
      return Collections.enumeration((Collection<String>)delegate.invokeWrappedMethod(delegate.fnGetHeaderNames));
    } catch (Exception e) {
      // Fall through
    }

    return Collections.enumeration(Collections.emptyList());
  }

  /**
   * Non-standard utility method. Retrieves all headers.
   */
  default Map<String, String> getHeaders() {
    HashMap<String, String> headers = new HashMap<>();

    for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
      String headerName = (String) e.nextElement();
      headers.put(headerName, this.getHeader(headerName));
    }

    return headers;
  }
}