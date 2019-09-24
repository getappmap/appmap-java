package com.appland.appmap.debugger;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;


interface ITraceListener {
  void onClassRegistered(ReferenceType ref);

  void onExceptionThrown(ObjectReference ref);

  void onMethodInvoked(Method ref, Location location);

  void onMethodReturned(Method ref, Location location);

  void onSqlQuery();

  void onHttpRequest();
}