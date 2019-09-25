package com.appland.appmap.debugger;

import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

import java.util.List;


interface ITraceListener {
  void onClassRegistered(ReferenceType ref);

  void onExceptionThrown(ObjectReference ref);

  void onMethodInvoked(Method method, List<Value> argumentValues, Long threadId);

  void onMethodReturned(Method method, Value returnValue, Long threadId);

  void onSqlQuery();

  void onHttpRequest();
}