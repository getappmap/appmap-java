package com.appland.appmap.reflect.apache;

import java.util.List;

import com.appland.appmap.reflect.ReflectiveType;

public class URIBuilder extends ReflectiveType {
  private static String BUILD = "build";
  private static String GET_QUERY_PARAMS = "getQueryParams";
  private static String REMOVE_QUERY = "removeQuery";
  private static String SET_HOST = "setHost";
  private static String SET_PORT = "setPort";
  private static String SET_SCHEME = "setScheme";

  public URIBuilder(Object self) {
    super(self);
    addMethods(BUILD, GET_QUERY_PARAMS, REMOVE_QUERY);
    addMethod(SET_HOST, String.class);
    addMethod(SET_PORT, Integer.TYPE);
    addMethod(SET_SCHEME, String.class);
  }

  public Object build() {
    return invokeObjectMethod(BUILD);
  }

  public List<?> getQueryParams() {
    return (List<?>) invokeObjectMethod(GET_QUERY_PARAMS);
  }

  public URIBuilder removeQuery() {
    invokeVoidMethod(REMOVE_QUERY);
    return this;
  }

  public URIBuilder setHost(String host) {
    invokeVoidMethod(SET_HOST, host);
    return this;
  }

  public URIBuilder setPort(int port) {
    invokeVoidMethod(SET_PORT, port);
    return this;
  }

  public URIBuilder setScheme(String scheme) {
    invokeVoidMethod(SET_SCHEME, scheme);
    return this;
  }
}