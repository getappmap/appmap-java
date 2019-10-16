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
import jdk.jfr.EventFactory;
import javassist.CtBehavior;

import com.appland.appmap.data_structures.CodeObjectTree;
import com.appland.appmap.data_structures.EventCallStack;
import com.appland.appmap.output.IAppMapSerializer;
import com.appland.appmap.output.v1.AppMapSerializer;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;

public class TraceListenerRecord implements ITraceListener, IAppMapSerializer {
  private CodeObjectTree classMap = new CodeObjectTree();
  private EventCallStack events = new EventCallStack();
  private static TraceEventFactory eventFactory = new TraceEventFactory();

  public String serialize() {
    return new AppMapSerializer(classMap, events).serialize();
  }

  /**
   * @return the eventFactory
   */
  public static TraceEventFactory getEventFactory() {
    return eventFactory;
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
  public void onMethodInvoked(Integer methodId, Object selfValue, Object[] params) {
    // TODO: `selfValue` doesn't appear to always be correct -db
    Event event = TraceListenerRecord.getEventFactory()
        .create(methodId, EventType.CALL)
        .setReceiver(selfValue);

    if (TraceUtil.isDebugMode()) {
      System.out.printf("%s.%s got %d params", event.definedClass, event.methodId, params.length);
    }

    try {
      for (int i = 0; i < params.length; i++) {
          Value paramValue = event.parameters.get(i);
          paramValue.set(params[i]);
      }
    } catch (IndexOutOfBoundsException e) {
      if (TraceUtil.isDebugMode()) {
        System.err.println(
            String.format("onMethodInvoked: %s.%s expected %d params, got %d",
                event.definedClass,
                event.methodId,
                event.parameters.size(),
                params.length));
        System.err.print("params received:");
        for (Object param : params) {
          System.err.printf(" (%s) %s, ", param.getClass().getName(), param.toString());
        }
        System.err.print("\nparams recorded:");
        for (Value param : event.parameters) {
          System.err.printf(" %s %s,", param.classType, param.name);
        }
        System.err.print("\n");
      }
    } catch(NullPointerException e) {
      System.err.printf("null pointer: %s\n", e.getMessage());
    }

    events.add(event);
  }

  @Override
  public void onMethodReturned(Integer methodId, Object returnValue) {
    Event event = TraceListenerRecord.getEventFactory()
        .create(methodId, EventType.RETURN)
        .setReturnValue(returnValue);

    if (returnValue != null) {
      System.out.printf("%d -> %s returning %s %s\n",
          methodId,
          event.methodId,
          returnValue.getClass().toString(),
          returnValue.toString());
    }

    events.add(event);
  }

  @Override
  public void onSqlQuery() {
  }

  @Override
  public void onHttpRequest() {
  }
}