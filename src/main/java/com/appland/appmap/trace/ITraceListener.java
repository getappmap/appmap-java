package com.appland.appmap.trace;

import java.lang.reflect.Method;

import java.util.List;


interface ITraceListener {
  void onClassRegistered(Class classType);

  void onExceptionThrown(Exception exception);

  void onMethodInvoked(Method method, Object selfValue, Object[] params);

  void onMethodReturned(Method method, Object returnValue);

  void onSqlQuery();

  void onHttpRequest();
}