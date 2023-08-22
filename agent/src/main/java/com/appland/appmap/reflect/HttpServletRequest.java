package com.appland.appmap.reflect;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.appland.appmap.process.hooks.http.ServletContext;

public class HttpServletRequest extends ReflectiveType implements HttpHeaders {
  // Needs to match
  // org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
  private static final String RESOURCE_URL_PROVIDER = "org.springframework.web.servlet.resource.ResourceUrlProvider";

  private final HttpHeaderDelegate headerDelegate;

  private final String GET_METHOD = "getMethod";
  private final String GET_REQUEST_URI = "getRequestURI";
  private final String GET_PROTOCOL = "getProtocol";
  private final String GET_PARAMETER_MAP = "getParameterMap";
  private final String GET_ATTRIBUTE = "getAttribute";
  private final String SET_ATTRIBUTE = "setAttribute";
  private final String GET_ATTRIBUTE_NAMES = "getAttributeNames";
  private final String GET_SERVLET_CONTEXT = "getServletContext";
  private final String GET_SERVLET_PATH = "getServletPath";

  public HttpServletRequest(Object self) {
    super(self);
    this.headerDelegate = new HttpHeaderDelegate(self);
    
    addMethods(GET_METHOD, GET_REQUEST_URI, GET_PROTOCOL, GET_PARAMETER_MAP, GET_ATTRIBUTE_NAMES, GET_SERVLET_CONTEXT,
        GET_SERVLET_PATH);
    addMethod(GET_ATTRIBUTE, String.class);
    addMethod(SET_ATTRIBUTE, String.class, Object.class);
  }

  public String getMethod() {
    return invokeStringMethod(GET_METHOD);
  }

  public String getRequestURI() {
    return invokeStringMethod(GET_REQUEST_URI);
  }

  public String getProtocol() {
    return invokeStringMethod(GET_PROTOCOL);
  }

  public Map<String, String[]> getParameterMap() {
    return invokeMethod(GET_PARAMETER_MAP, new HashMap<String, String[]>());
  }

  public Object getAttribute(String name) {
    return invokeObjectMethod(GET_ATTRIBUTE, name);
  }

  public void setAttribute(String key, Object value) {
    invokeObjectMethod(SET_ATTRIBUTE, key, value);
  }
  
  @SuppressWarnings("unchecked")
  public Enumeration<String> getAttributeNames() {
    return (Enumeration<String>) invokeObjectMethod(GET_ATTRIBUTE_NAMES);
  }

  public ServletContext getServletContext() {
    return new ServletContext(invokeObjectMethod(GET_SERVLET_CONTEXT));
  }

  public String getServletPath() {
    return invokeStringMethod(GET_SERVLET_PATH);
  }

  @Override
  public HttpHeaderDelegate getHeaderDelegate() {
    return headerDelegate;
  }

  private static class ResourceUrlProvider extends ReflectiveType {
    private static String GET_FOR_LOOKUP_PATH = "getForLookupPath";

    public ResourceUrlProvider(Object self) {
      super(self);
      addMethod(GET_FOR_LOOKUP_PATH, String.class);
    }

    public String getForLookupPath(String path) {
      return invokeStringMethod(GET_FOR_LOOKUP_PATH, path);
    }
  }

  public boolean isForStaticResource() {
    Object obj = getAttribute(RESOURCE_URL_PROVIDER);
    if (obj == null) {
      return false;
    }
    ResourceUrlProvider provider = new ResourceUrlProvider(obj);
    return provider.getForLookupPath(getServletPath()) != null;
  }
}
