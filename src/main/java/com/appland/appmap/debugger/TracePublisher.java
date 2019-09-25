package com.appland.appmap.debugger;

import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

import java.util.ArrayList;
import java.util.List;

public class TracePublisher implements ITraceListener {

  private ArrayList<ITraceListener> listeners = new ArrayList<ITraceListener>();

  protected void addEventListener(ITraceListener listener) {
    listeners.add(listener);
  }

  @Override
  public void onClassRegistered(ReferenceType ref) {
    for (ITraceListener listener : listeners) {
      listener.onClassRegistered(ref);
    }
  }

  @Override
  public void onExceptionThrown(ObjectReference ref) {
    for (ITraceListener listener : listeners) {
      listener.onExceptionThrown(ref);
    }
  }

  @Override
  public void onMethodInvoked(Method method, List<Value> argumentValues, Long threadId) {
    for (ITraceListener listener : listeners) {
      listener.onMethodInvoked(method, argumentValues, threadId);
    }
  }

  @Override
  public void onMethodReturned(Method method, Value returnValue, Long threadId) {
    for (ITraceListener listener : listeners) {
      listener.onMethodReturned(method, returnValue, threadId);
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