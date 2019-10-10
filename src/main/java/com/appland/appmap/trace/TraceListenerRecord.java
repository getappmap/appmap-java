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
import java.util.HashMap;
import javassist.CtClass;
import javassist.CtBehavior;

import com.appland.appmap.data_structures.CodeObjectTree;
import com.appland.appmap.data_structures.EventCallStack;
import com.appland.appmap.output.IAppMapSerializer;
import com.appland.appmap.output.v1.AppMapSerializer;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class TraceListenerRecord implements ITraceListener, IAppMapSerializer {
  class MethodLocation {
    String path;
    Integer lineNumber;

    public MethodLocation(String path, Integer lineNumber) {
      this.path = path;
      this.lineNumber = lineNumber;
    }
  }

  private CodeObjectTree classMap = new CodeObjectTree();
  private EventCallStack events = new EventCallStack();
  private HashMap<String, MethodLocation> methodLocations = new HashMap<String, MethodLocation>();

  private String behaviorKey(CtBehavior behavior) {
    return String.format("%s.%s:%d",
        behavior.getDeclaringClass().getName(),
        behavior.getName(),
        behavior.getMethodInfo().getLineNumber(0));
  }

  private void registerBehaviorLocation(CtBehavior behavior) {
    String key = String.format("%s::%s",
        behavior.getDeclaringClass().getName(),
        behavior.getName());

    MethodLocation val = new MethodLocation(
        TraceUtil.getSourcePath(behavior.getDeclaringClass()),
        behavior.getMethodInfo().getLineNumber(0));

    methodLocations.put(key, val);
  }

  private MethodLocation getBehaviorLocation(Method behavior) {
    return methodLocations.get(String.format("%s::%s",
        behavior.getDeclaringClass().getName(),
        behavior.getName()));
  }

  public String serialize() {
    return new AppMapSerializer(classMap, events).serialize();
  }

  @Override
  public void onClassLoad(CtClass classType) {
    CodeObject rootObject = CodeObject.createTree(classType);
    CodeObject classObject = rootObject.get(classType.getName());

    CtBehavior[] behaviors = classType.getDeclaredBehaviors();
    for (CtBehavior behavior : behaviors) {
      if (TraceUtil.isRelevant(behavior) == false) {
        continue;
      }

      this.registerBehaviorLocation(behavior);
      CodeObject behaviorObject = new CodeObject(behavior);
      classObject.addChild(behaviorObject);
    }

    if (classObject.children.size() > 0) {
      classMap.add(rootObject);
    }
  }

  @Override
  public void onExceptionThrown(Exception exception) {
  }

  @Override
  public void onMethodInvoked(Method method, Object selfValue, Object[] params) {
    // TODO: `selfValue` doesn't appear to always be correct -db
    Event event = new Event(method, "call")
        .setReceiver(selfValue);

    MethodLocation location = getBehaviorLocation(method);
    if (location != null) {
      event.setLineNumber(location.lineNumber)
          .setPath(location.path);
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
    Event event = new Event(method, "return")
        .setReturnValue(returnValue);

    events.add(event);
  }

  @Override
  public void onSqlQuery() {
  }

  @Override
  public void onHttpRequest() {
  }
}