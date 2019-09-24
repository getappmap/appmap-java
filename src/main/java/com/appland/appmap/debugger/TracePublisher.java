package com.appland.appmap.debugger;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

import java.util.ArrayList;

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
  public void onMethodInvoked(Method ref, Location location) {
    for (ITraceListener listener : listeners) {
      listener.onMethodInvoked(ref, location);
    }
  }

  @Override
  public void onMethodReturned(Method ref, Location location) {
    for (ITraceListener listener : listeners) {
      listener.onMethodReturned(ref, location);
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