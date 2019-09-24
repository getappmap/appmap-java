package com.appland.appmap.debugger;

import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Method;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ExceptionEvent;

import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ExceptionRequest;

import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;

public class Trace implements ITraceListener {

  TraceBuilder builder = new TraceBuilder();

  public void execute(String mainClass, String classPath) {
    builder
      .addListener(this)
      .mainClass(mainClass)
      .classPath(classPath)
      .launch();
  }

  public void execute(String mainClass) {
    builder
      .addListener(this)
      .addClassFilter("com.appland.*")
      .mainClass(mainClass)
      .launch();
  }

  public void execute(File jarFile) {
    throw new UnsupportedOperationException("not yet implemented");

    // builder
    //   .addListener(this)
    //   .jarFile(jarFile.getAbsolutePath())
    //   .launch();
  }

  @Override
  public void onClassRegistered(ReferenceType ref) {
    try {
      System.out.println(ref.name());
      List<Method> methods = ref.methods();
      for (Method method : methods) {
        System.out.println(
            String.format("\t %s\t%s:%s",
              method.name(),
              method.location().sourceName(),
              method.location().lineNumber()));
      }
    } catch (ClassNotPreparedException e) {
      System.err.println(String.format("%s not yet prepared", ref.name()));
    } catch (AbsentInformationException e) {
      System.err.println(String.format("%s not yet prepared", ref.name()));
    }
  }

  @Override
  public void onExceptionThrown(ObjectReference ref) {

  }

  @Override
  public void onMethodInvoked(Method ref, Location location) {

  }

  @Override
  public void onMethodReturned(Method ref, Location location) {

  }

  @Override
  public void onSqlQuery() {

  }

  @Override
  public void onHttpRequest() {

  }
}