package com.appland.appmap.trace;

import java.lang.reflect.Method;
import javassist.CtClass;

import java.util.List;

public class TraceListenerDebug implements ITraceListener {

  @Override
  public void onClassLoad(CtClass classType) {
    System.out.println(String.format("onClassRegistered: %s", classType.getSimpleName()));
  }

  @Override
  public void onExceptionThrown(Exception exception) {
    System.out.println(String.format("onExceptionThrown: %s", exception.getMessage()));
  }

  @Override
  public void onMethodInvoked(Integer methodId, Object selfValue, Object[] params) {
    System.out.println(
        String.format("onMethodInvoked: %d", methodId));
  }

  @Override
  public void onMethodReturned(Integer methodId, Object returnValue) {
    System.out.println(
        String.format("onMethodReturned: %d", methodId));
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