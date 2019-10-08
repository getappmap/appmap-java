package com.appland.appmap.trace;

import java.lang.reflect.Method;

import java.util.List;

public class TraceListenerDebug implements ITraceListener {

  @Override
  public void onClassRegistered(Class classType) {
    System.out.println(String.format("onClassRegistered: %s", classType.getSimpleName()));
  }

  @Override
  public void onExceptionThrown(Exception exception) {
    System.out.println(String.format("onExceptionThrown: %s", exception.getMessage()));
  }

  @Override
  public void onMethodInvoked(Method method, Object selfValue, Object[] params) {
    System.out.println(
        String.format("onMethodInvoked: %s.%s",
            method.getDeclaringClass().getName(),
            method.getName()));
  }

  @Override
  public void onMethodReturned(Method method, Object returnValue) {
    System.out.println(
        String.format("onMethodReturned: %s.%s",
            method.getDeclaringClass().getName(),
            method.getName()));
  }

  @Override
  public void onSqlQuery() {
    System.out.println(String.format("onSqlQuery"));
  }

  @Override
  public void onHttpRequest() {
    System.out.println(String.format("onHttpRequest"));
  }
}