package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Parameters;
import com.appland.appmap.output.v1.NoSourceAvailableException;
import com.appland.appmap.output.v1.Value;
import java.lang.reflect.Modifier;
import java.util.Vector;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class EventTemplateRegistry {
  private static EventTemplateRegistry instance = new EventTemplateRegistry();
  private static final Recorder recorder = Recorder.getInstance();

  private Vector<Event> eventTemplates = new Vector<Event>();

  private EventTemplateRegistry() { }

  public static EventTemplateRegistry get() {
    return EventTemplateRegistry.instance;
  }

  public Integer register(CtBehavior behavior) {
    Event event = new Event(behavior);
    return this.register(event, behavior);
  }

  public Integer register(Event event, CtBehavior behavior) {
    recorder.register(CodeObject.createTree(behavior));
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

  public Event cloneEventTemplate(int templateId, String eventAction)
      throws UnknownEventException {
    Event event = null;

    try {
      Event eventTemplate = eventTemplates.get(templateId);
      event = new Event(eventTemplate)
          .setThreadId(Thread.currentThread().getId())
          .setEvent(eventAction);

      if (eventAction.equals("call")) {
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
