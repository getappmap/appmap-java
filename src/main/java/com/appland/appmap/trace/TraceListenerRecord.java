package com.appland.appmap.trace;

import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.appland.appmap.data_structures.CodeObjectTree;
import com.appland.appmap.data_structures.EventCallStack;
import com.appland.appmap.output.IAppMapSerializer;
import com.appland.appmap.output.v1.AppMapSerializer;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class TraceListenerRecord implements ITraceListener, IAppMapSerializer {
  private CodeObjectTree classMap = new CodeObjectTree();
  private EventCallStack events = new EventCallStack();

  public String serialize() {
    return new AppMapSerializer(classMap, events).serialize();
  }

  @Override
  public void onClassRegistered(Class classType) {
    CodeObject rootObject = CodeObject.createTree(classType);
    CodeObject classObject = rootObject.get(classType.getName());

    Constructor[] constructors = classType.getConstructors();
    for (Constructor constructor : constructors) {
      if (constructor.isSynthetic()) {
        continue;
      }

      CodeObject constructorObject = new CodeObject(constructor);
      classObject.addChild(constructorObject);
    }

    Method[] methods = classType.getDeclaredMethods();
    for (Method method : methods) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }

      if (method.isSynthetic()) {
        continue;
      }

      CodeObject methodObject = new CodeObject(method);
      classObject.addChild(methodObject);
    }

    classMap.add(rootObject);
  }

  @Override
  public void onExceptionThrown(Exception exception) {
  }

  @Override
  public void onMethodInvoked(Method method, Object selfValue, Object[] params) {
    this.onClassRegistered(method.getDeclaringClass());

    // TODO: `selfValue` doesn't appear to always be correct -db
    Event event = new Event(method, "call").setReceiver(selfValue);
    if (params != null) {
      System.out.println(String.format("%s.%s received %d arguments", method.getDeclaringClass().getName(), method.getName(), params.length));
    } else {
      System.out.println(String.format("%s.%s received no arguments", method.getDeclaringClass().getName(), method.getName()));
    }
    Parameter[] paramInfo;
    try {
      paramInfo = method.getParameters();
      for (int i = 0; i < params.length; ++i) {
        event.addParameter(params[i], paramInfo[i].getName());
      }
    } catch (MalformedParametersException e) {
      System.err.println(
          String.format(
              "failed to get parameters for method invocation %s.%s:\n%s",
              method.getDeclaringClass().getName(),
              method.getName(),
              e.getMessage()));
    }

    events.add(event);
  }

  @Override
  public void onMethodReturned(Method method, Object returnValue) {
    Event event = new Event(method, "return").setReturnValue(returnValue);
    events.add(event);
  }

  @Override
  public void onSqlQuery() {
  }

  @Override
  public void onHttpRequest() {
  }
}