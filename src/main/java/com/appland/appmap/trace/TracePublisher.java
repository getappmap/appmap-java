package com.appland.appmap.trace;

import javassist.CtClass;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

public class TracePublisher implements ITraceListener {

  private ArrayList<ITraceListener> listeners = new ArrayList<ITraceListener>();

  protected void addEventListener(ITraceListener listener) {
    listeners.add(listener);
  }

  @Override
  public void onClassLoad(CtClass classType) {
    for (ITraceListener listener : listeners) {
      listener.onClassLoad(classType);
    }
  }

  @Override
  public void onExceptionThrown(Exception exception) {
    for (ITraceListener listener : listeners) {
      listener.onExceptionThrown(exception);
    }
  }

  @Override
  public void onMethodInvoked(Method method, Object selfValue, Object[] params) {
    for (ITraceListener listener : listeners) {
      listener.onMethodInvoked(method, selfValue, params);
    }
  }

  @Override
  public void onMethodReturned(Method method, Object returnValue) {
    for (ITraceListener listener : listeners) {
      listener.onMethodReturned(method, returnValue);
    }
  }

  @Override
  public void onSqlQuery() {
    for (ITraceListener listener : listeners) {
      listener.onSqlQuery();
    }
  }

  @Override
  public void onHttpRequest() {
    for (ITraceListener listener : listeners) {
      listener.onHttpRequest();
    }
  }
}