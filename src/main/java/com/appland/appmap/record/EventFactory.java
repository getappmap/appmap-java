package com.appland.appmap.record;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import java.lang.reflect.Modifier;
import java.util.Vector;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class EventFactory {
  private static Integer eventId = 0;
  private static EventFactory instance = new EventFactory();
  private Vector<Event> eventTemplates = new Vector<Event>();

  private EventFactory() { }

  public static EventFactory get() {
    return EventFactory.instance;
  }

  private Integer getEventId() {
    return eventId++;
  }

  private static String getSourcePath(CtClass classType) {
    String srcPath = classType.getName().replace('.', '/');
    return String.format("src/main/java/%s.java", srcPath);
  }

  public Integer register(CtBehavior behavior) {
    Event event = new Event()
        .setDefinedClass(behavior.getDeclaringClass().getName())
        .setMethodId(behavior.getName())
        .setStatic((behavior.getModifiers() & Modifier.STATIC) != 0)
        .setPath(EventFactory.getSourcePath(behavior.getDeclaringClass()))
        .setLineNumber(behavior.getMethodInfo().getLineNumber(0));

    MethodInfo methodInfo = behavior.getMethodInfo();
    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    LocalVariableAttribute locals = (LocalVariableAttribute) codeAttribute.getAttribute(
        javassist.bytecode.LocalVariableAttribute.tag);

    Integer numberLocals = locals.tableLength();
    CtClass[] parameterTypes = new CtClass[]{};

    try {
      parameterTypes = behavior.getParameterTypes();
    } catch (NotFoundException e) {
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
      // if (TraceUtil.isDebugMode()) {
      //   System.out.printf("- %s\t%s\n",
      //       param.classType,
      //       param.name);
      // }
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

  public Event create(Integer templateId, EventAction eventAction) throws UnknownEventException {
    Event event = null;

    try {
      Event eventTemplate = eventTemplates.get(templateId);
      event = new Event(eventTemplate)
          .setThreadId(Thread.currentThread().getId())
          .setEvent(eventAction == EventAction.CALL ? "call" : "return");

      if (eventAction == EventAction.CALL) {
        for (Value param : eventTemplate.parameters) {
          event.addParameter(param);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new UnknownEventException(String.format("unknown template for ordinal %d", templateId));
    }
    
    return event;
  }
}
