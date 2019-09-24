package com.appland.appmap.debugger;

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
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;

public class TraceBuilder extends TracePublisher {

  private LaunchingConnector launchingConnector = Bootstrap
      .virtualMachineManager()
      .defaultConnector();

  private ArrayList<String> classFilters = new ArrayList<String>();
  private Map<String, Argument> arguments;

  public TraceBuilder() {
    arguments = launchingConnector.defaultArguments();
    classPath(System.getProperty("java.class.path"));
  }

  public TraceBuilder mainClass(String mainClass) {
    arguments.get("main").setValue(mainClass);

    return this;
  }

  public TraceBuilder classPath(String classPath) {
    arguments.get("options").setValue(String.format("-cp %s", classPath));

    return this;
  }

  public TraceBuilder jarFile(String jarFile) {
    arguments.get("options").setValue(String.format("-jar %s", jarFile));

    return this;
  }

  public TraceBuilder addListener(ITraceListener listener) {
    addEventListener(listener);
    return this;
  }

  public TraceBuilder addClassFilter(String classFilter) {
    classFilters.add(classFilter);
    return this;
  }


  public void launch() {
    VirtualMachine vm = null;
    try {
      vm = launchingConnector.launch(arguments);
    } catch (IOException e) {
      System.err.println("unknown exception");
      return;
    } catch (IllegalConnectorArgumentsException e) {
      System.err.println(String.format("bad connector arguments: %s", e));
      return;
    } catch (VMStartException e) {
      System.err.println(String.format("vm start failed: %s", e));
      return;
    }

    EventRequestManager requestManager = vm.eventRequestManager();

    for (String classFilter : classFilters) {
      ClassPrepareRequest classPrepareRequest = requestManager.createClassPrepareRequest();
      classPrepareRequest.addClassFilter(classFilter);
      classPrepareRequest.enable();
    }

    Boolean started = false;
    EventQueue eventQueue = vm.eventQueue();
    for (;;) {
      EventSet events = null;
      try {
        events = eventQueue.remove();
      } catch (InterruptedException e) {
        System.out.println("this thread has been interrupted");
      } catch (VMDisconnectedException e) {
        System.out.println("the vm has disconnected");
        return;
      }

      
      for (Event event : events) {
        if (event instanceof VMStartEvent) {
          started = true;
        }

        if (started == false) {
          break;
        }

        if (event instanceof ExceptionEvent) {
          onExceptionThrown(((ExceptionEvent) event).exception());
        }

        if (event instanceof ClassPrepareEvent) {
          onClassRegistered(((ClassPrepareEvent) event).referenceType());
        }
      }

      events.resume();
    }
  }

  public LaunchingConnector get() {
    return launchingConnector;
  }
}