package com.appland.appmap.debugger;

import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Method;
import com.sun.jdi.Value;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;

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
import java.util.Vector;
import java.util.List;

import com.appland.appmap.output.IAppMapSerializer;
import com.appland.appmap.output.v1.AppMapSerializer;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class Trace implements ITraceListener {

  private TraceBuilder builder = new TraceBuilder();
  private IAppMapSerializer serializer = new AppMapSerializer();

  public void execute(String main, String classPath) {
    builder
      .addListener(this)
      .main(main)
      .classPath(classPath)
      .launch();
  }

  public void execute(String main) {
    builder
      .addListener(this)
      .main(main)
      .launch();
  }

  public void execute(File jarFile, String[] args) {
    throw new UnsupportedOperationException("not yet implemented");

    // builder
    //   .addListener(this)
    //   .jarFile(jarFile.getAbsolutePath())
    //   .launch();
  }

  public String serialize() {
    if (serializer == null) {
      return "";
    }

    return serializer.serialize();
  }

  public Trace includeClassPath(String classPath) {
    builder.addClassFilter(classPath);
    return this;
  }

  public Trace excludeClassPath(String classPath) {
    builder.addClassExclusionFilter(classPath);
    return this;
  }


  private static String stripPackage(String fullyQualifiedName) {
    Integer classNameIndex = fullyQualifiedName.lastIndexOf('.') + 1;
    return fullyQualifiedName.substring(classNameIndex);
  }

  private static String stripClass(String fullyQualifiedName) {
    Integer classNameIndex = fullyQualifiedName.lastIndexOf('.');
    return fullyQualifiedName.substring(0, classNameIndex);
  }

  private static String getPackagePath(String fullyQualifiedName) {
    return stripClass(fullyQualifiedName).replace('.', '/');
  }

  private static String getFullSourcePath(String basePath, String fileName) {
    return String.format("src/main/java/%s/%s", basePath, fileName);
  }

  private static String getLocationString(String sourcePath, Location location) {
    String locationString = "";

    try {
      String fullSourcePath = getFullSourcePath(sourcePath, location.sourceName());
      locationString = String.format("%s:%d", fullSourcePath, location.lineNumber());
    } catch (AbsentInformationException e) {
      // do nothing
      // the location information is not available
    }

    return locationString;
  }

  private void registerMethodBreakpoints(Method method) {

  }

  @Override
  public void onClassRegistered(ReferenceType ref) {
    try {
      String packagePath = getPackagePath(ref.name());

      CodeObject packageObject = new CodeObject();
      packageObject.name = stripClass(ref.name());
      packageObject.type = "package";
      packageObject.location = String.format("src/main/java/%s", packagePath);
      // todo: add package to code objects list
      //       merge with existing objects
      //       maybe use Map<String (name), CodeObject>
      //       and merge children

      String className = stripPackage(ref.name());

      CodeObject classObj = new CodeObject();
      classObj.name = className;
      classObj.type = "class";

      packageObject.children = new CodeObject[]{ classObj };

      try {
        Location classLocation = ref.allLineLocations().get(0);
        classObj.location = getLocationString(packagePath, classLocation);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
        // the location information is not available
      }

      Vector<CodeObject> children = new Vector<CodeObject>();
      List<Method> methods = ref.methods();
      for (Method method : methods) {
        if (method.isPublic() == false || method.isSynthetic()) {
          continue;
        }

        String methodName = method.name();
        if (method.isConstructor()) {
          methodName = className;
        }

        CodeObject methodObj = new CodeObject();
        methodObj.name = methodName;
        methodObj.type = "function";
        methodObj.location = getLocationString(packagePath, method.location());

        children.add(methodObj);
      }

      classObj.children = new CodeObject[children.size()];
      children.copyInto(classObj.children);

      serializer.addCodeObject(packageObject);
    } catch (ClassNotPreparedException e) {
      System.err.println(String.format("%s not yet prepared", ref.name()));
    } catch (AbsentInformationException e) {
      System.err.println(String.format("%s not yet prepared", ref.name()));
    }
  }

  @Override
  public void onExceptionThrown(ObjectReference ref) {
    System.out.println("an exception was thrown!");
    System.out.println(ref.referenceType().name());
  }

  @Override
  public void onMethodInvoked(Method method, List<Value> argumentValues, Long threadId) {
    Event event = new Event();
    event.event = "call";
    event.definedClass = method.declaringType().name();
    event.methodId = method.name();
    event.lineNumber = method.location().lineNumber();
    event.isStatic = method.isStatic();
    event.threadId = threadId;

    if (method.isConstructor()) {
      event.methodId = stripPackage(method.declaringType().name());
    }

    serializer.addEvent(event);
  }

  @Override
  public void onMethodReturned(Method method, Value returnValue, Long threadId) {
    Event event = new Event();
    event.event = "return";
    event.definedClass = method.declaringType().name();
    event.methodId = method.name();
    event.lineNumber = method.location().lineNumber();
    event.isStatic = method.isStatic();
    event.threadId = threadId;

    if (method.isConstructor()) {
      event.methodId = stripPackage(method.declaringType().name());
    }

    serializer.addEvent(event);
  }

  @Override
  public void onSqlQuery() {

  }

  @Override
  public void onHttpRequest() {

  }
}