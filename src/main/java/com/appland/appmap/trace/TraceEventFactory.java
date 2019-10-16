package com.appland.appmap.trace;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.appland.appmap.output.IAppMapSerializer;
import com.appland.appmap.output.v1.AppMapSerializer;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtBehavior;
import javassist.CtNewMethod;
import javassist.CtConstructor;
import javassist.CannotCompileException;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.LocalVariableAttribute;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.ReflectionUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;

enum EventType {
  CALL,
  RETURN
};

public class TraceEventFactory {
  private Vector<Event> eventTemplates = new Vector<Event>();
  private static Integer eventId = 0;

  private Integer getEventId() {
    return eventId++;
  }

  public Integer register(CtBehavior behavior) {
    Event event = new Event()
        .setDefinedClass(behavior.getDeclaringClass().getName())
        .setMethodId(behavior.getName())
        .setStatic((behavior.getModifiers() & Modifier.STATIC) != 0)
        .setPath(TraceUtil.getSourcePath(behavior.getDeclaringClass()))
        .setLineNumber(behavior.getMethodInfo().getLineNumber(0));

    MethodInfo methodInfo = behavior.getMethodInfo();
    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    LocalVariableAttribute locals = (LocalVariableAttribute) codeAttribute.getAttribute(
        javassist.bytecode.LocalVariableAttribute.tag);

    Integer numberLocals = locals.tableLength();
    CtClass[] parameterTypes = new CtClass[]{};

    try {
      parameterTypes = behavior.getParameterTypes();
      System.out.printf("%s has %d params:\n", behavior.getName(), parameterTypes.length);
    } catch(NotFoundException e) {
      System.err.println(
          String.format("failed to get parameter types for %s.%s: %s",
              event.definedClass,
              event.methodId,
              e.getMessage()));
    }

    Value[] params = new Value[parameterTypes.length];

    for (int i = 0; i < numberLocals; ++i) {
      Integer localIndex = locals.index(i);
      if (localIndex > parameterTypes.length) {
        continue;
      }

      if (event.isStatic == false) {
        if (localIndex == 0) {
          // index 0 is `this` for nonstatic methods
          // we don't need it
          continue;
        } else {
          // shift back by one to account for `this`
          localIndex -= 1;
        }
      }

      Value param = new Value()
          .setClassType(parameterTypes[localIndex].getName())
          .setName(locals.variableName(i))
          .setKind("req");

      params[localIndex] = param;
    }

    for (int i = 0; i < params.length; ++i) {
      Value param = params[i];
      if (TraceUtil.isDebugMode()) {
        System.out.printf("- %s\t%s\n",
            param.classType,
            param.name);
      }
      event.addParameter(param);
    }

    eventTemplates.add(event);
    return eventTemplates.size() - 1;
  }

  public Event getTemplate(Integer templateId) {
    try {
      return eventTemplates.get(templateId);
    } catch (ArrayIndexOutOfBoundsException e) {
      // fall through
    }
    return null;
  }

  public Event create(Integer templateId, EventType eventType) {
    Event event = null;

    try {
      Event eventTemplate = eventTemplates.get(templateId);
      event = new Event(eventTemplate)
          .setId(getEventId())
          .setThreadId(Thread.currentThread().getId())
          .setEvent(eventType == EventType.CALL ? "call" : "return");

      if (eventType == EventType.CALL) {
        for (Value param : eventTemplate.parameters) {
          event.addParameter(param);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println(e.getMessage());
    }
    
    return event;
  }
}