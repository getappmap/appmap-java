package com.appland.appmap.trace;

import java.lang.reflect.Method;
import javassist.CtClass;

import java.util.List;


interface ITraceListener {
  void onClassLoad(CtClass classType);

  void onExceptionThrown(Exception exception);

  void onMethodInvoked(Method method, Object selfValue, Object[] params);

  void onMethodReturned(Method method, Object returnValue);

  void onSqlQuery();

  void onHttpRequest();
}