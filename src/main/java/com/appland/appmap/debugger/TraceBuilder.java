// package com.appland.appmap.debugger;

// import com.sun.jdi.ReferenceType;
// import com.sun.jdi.Method;
// import com.sun.jdi.Bootstrap;
// import com.sun.jdi.IncompatibleThreadStateException;
// import com.sun.jdi.StackFrame;
// import com.sun.jdi.ThreadReference;
// import com.sun.jdi.Value;
// import com.sun.jdi.VirtualMachine;
// import com.sun.jdi.VMDisconnectedException;
// import com.sun.jdi.AbsentInformationException;
// import com.sun.jdi.ClassNotPreparedException;

// import com.sun.jdi.event.ClassPrepareEvent;
// import com.sun.jdi.event.Event;
// import com.sun.jdi.event.EventQueue;
// import com.sun.jdi.event.EventSet;
// import com.sun.jdi.event.ExceptionEvent;
// import com.sun.jdi.event.MethodEntryEvent;
// import com.sun.jdi.event.MethodExitEvent;
// import com.sun.jdi.event.VMStartEvent;
// import com.sun.jdi.event.VMDeathEvent;
// import com.sun.jdi.event.VMDisconnectEvent;


// import com.sun.jdi.request.MethodEntryRequest;
// import com.sun.jdi.request.MethodExitRequest;
// import com.sun.jdi.request.EventRequestManager;
// import com.sun.jdi.request.ClassPrepareRequest;
// import com.sun.jdi.request.ExceptionRequest;

// import com.sun.jdi.connect.Connector;
// import com.sun.jdi.connect.Connector.Argument;
// import com.sun.jdi.connect.IllegalConnectorArgumentsException;
// import com.sun.jdi.connect.LaunchingConnector;
// import com.sun.jdi.connect.VMStartException;

// import java.io.File;
// import java.io.IOException;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;

// public class TraceBuilder extends TracePublisher {

//   private LaunchingConnector launchingConnector = Bootstrap
//       .virtualMachineManager()
//       .defaultConnector();

//   private EventRequestManager requestManager = null;

//   private ArrayList<String> classFilters = new ArrayList<String>();
//   private ArrayList<String> classExclusionFilters = new ArrayList<String>();
//   private Map<String, Argument> arguments;

//   public TraceBuilder() {
//     arguments = launchingConnector.defaultArguments();
//     classPath(System.getProperty("java.class.path"));

//     if (System.getenv("DEBUG") != null) {
//       addListener(new TraceListenerDebug());
//     }
//   }

//   public TraceBuilder main(String val) {
//     arguments.get("main").setValue(val);

//     return this;
//   }

//   public TraceBuilder classPath(String classPath) {
//     arguments.get("options").setValue(String.format("-cp %s", classPath));

//     return this;
//   }

//   public TraceBuilder jarFile(String jarFile) {
//     System.out.println(String.format("classpath set to %s:%s", System.getProperty("java.class.path"), jarFile));
//     arguments.get("options").setValue(
//         String.format("-cp %s:%s", System.getProperty("java.class.path"), jarFile));

//     return this;
//   }

//   public TraceBuilder addListener(ITraceListener listener) {
//     addEventListener(listener);
//     return this;
//   }

//   public TraceBuilder addClassFilter(String classFilter) {
//     classFilters.add(classFilter);
//     return this;
//   }

//   public TraceBuilder addClassExclusionFilter(String classFilter) {
//     classExclusionFilters.add(classFilter);
//     return this;
//   }

//   public void launch() {
//     VirtualMachine vm = null;
//     try {
//       vm = launchingConnector.launch(arguments);

//       // TODO: the VM output and error streams must be read as it executes.
//       // These streams are available through the Process object returned by
//       // VirtualMachine.process(). If the streams are not periodically read,
//       // the target VM will stop executing when the buffers for these streams
//       // are filled.
//     } catch (IOException e) {
//       System.err.println("unknown exception");
//       return;
//     } catch (IllegalConnectorArgumentsException e) {
//       System.err.println(String.format("bad connector arguments: %s", e));
//       return;
//     } catch (VMStartException e) {
//       System.err.println(String.format("vm start failed: %s", e));
//       return;
//     }

//     requestManager = vm.eventRequestManager();

//     for (String classFilter : classFilters) {
//       ClassPrepareRequest classPrepareRequest = requestManager.createClassPrepareRequest();
//       classPrepareRequest.addClassFilter(classFilter);
//       classPrepareRequest.enable();

//       MethodEntryRequest methodEntryRequest = requestManager.createMethodEntryRequest();
//       methodEntryRequest.addClassFilter(classFilter);
//       methodEntryRequest.enable();

//       MethodExitRequest methodExitRequest = requestManager.createMethodExitRequest();
//       methodExitRequest.addClassFilter(classFilter);
//       methodExitRequest.enable();
//     }

//     for (String classFilter : classExclusionFilters) {
//       ClassPrepareRequest classPrepareRequest = requestManager.createClassPrepareRequest();
//       classPrepareRequest.addClassExclusionFilter(classFilter);
//       classPrepareRequest.enable();

//       MethodEntryRequest methodEntryRequest = requestManager.createMethodEntryRequest();
//       methodEntryRequest.addClassExclusionFilter(classFilter);
//       methodEntryRequest.enable();

//       MethodExitRequest methodExitRequest = requestManager.createMethodExitRequest();
//       methodExitRequest.addClassExclusionFilter(classFilter);
//       methodExitRequest.enable();
//     }

//     // TODO: capture exceptions
//     //
//     // a method will not appear as exiting if an unhandled exception was thrown.
//     // it appears necessary to:
//     // 1. listen for the exception event
//     // 2. suspend the current thread
//     // 3. walk the stack for methods which are currently running until the
//     //    events catchLocation is reached.
//     // 4. mark those methods as 'return'
//     //
//     // though, this method is not guaranteed to produce intended results. see
//     // https://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/com/sun/jdi/event/ExceptionEvent.html#catchLocation()
//     // for additional information.
//     //
//     // ExceptionRequest exceptionRequest = requestManager.createExceptionRequest(null, true, false);
//     // exceptionRequest.enable();

//     ProcessOutput processOutput = null;
//     Boolean started = false;
//     EventQueue eventQueue = vm.eventQueue();
//     for (;;) {
//       EventSet events = null;
//       try {
//         events = eventQueue.remove();
//       } catch (InterruptedException e) {
//         System.out.println("this thread has been interrupted");
//       } catch (VMDisconnectedException e) {
//         System.out.println("the vm has disconnected");
//         return;
//       }

      
//       for (Event event : events) {
//         if (event instanceof VMStartEvent) {
//           processOutput = new ProcessOutput(vm.process());
//           Thread outputThread = new Thread(processOutput);
//           outputThread.start();
//           started = true;
//         } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
//           if (processOutput != null) {
//             processOutput.stop();
//           }
//           return;
//         }

//         if (started == false) {
//           break;
//         }

//         if (event instanceof ExceptionEvent) {
//           onExceptionThrown(((ExceptionEvent) event).exception());
//         } else if (event instanceof ClassPrepareEvent) {
//           onClassRegistered(((ClassPrepareEvent) event).referenceType());
//         } else if (event instanceof MethodEntryEvent) {
//           MethodEntryEvent entryEvent = (MethodEntryEvent) event;
//           if (entryEvent.method().isPublic() == false) {
//             continue;
//           }

//           if (entryEvent.method().isSynthetic()) {
//             continue;
//           }

//           ThreadReference currentThread = entryEvent.thread();
//           List<Value> argumentValues = new ArrayList<Value>();

//           // currentThread.suspend();

//           // try {
//           //   if (currentThread.frameCount() > 0) {
//           //     StackFrame stack = currentThread.frame(0);
//           //     argumentValues = stack.getArgumentValues();
//           //   }
//           // } catch (IncompatibleThreadStateException e) {
//           //   System.err.println(String.format(
//           //       "error: could not examine stack when handling method entry for %s.",
//           //       entryEvent.method().name()));
//           //   System.err.println("       the current thread is not suspended.");
//           //   currentThread.resume();
//           //   continue;
//           // }

//           // currentThread.resume();

//           onMethodInvoked(entryEvent.method(),
//               argumentValues,
//               currentThread.uniqueID());
//         } else if (event instanceof MethodExitEvent) {
//           MethodExitEvent exitEvent = (MethodExitEvent) event;
//           // if (exitEvent.method().isPublic() == false) {
//           //   continue;
//           // }

//           // if (exitEvent.method().isSynthetic()) {
//           //   continue;
//           // }

//           onMethodReturned(exitEvent.method(),
//               exitEvent.returnValue(),
//               exitEvent.thread().uniqueID());
//         }
//       }

//       events.resume();
//     }
//   }

//   public LaunchingConnector get() {
//     return launchingConnector;
//   }
// }