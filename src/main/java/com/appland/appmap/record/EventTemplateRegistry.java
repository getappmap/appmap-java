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

/**
 * Stores events as templates built from behaviors intended to be hooked. Hooks can then access and
 * clone these templates before storing runtime information to be recorded.
 */
public class EventTemplateRegistry {
  private static EventTemplateRegistry instance = new EventTemplateRegistry();
  private static final Recorder recorder = Recorder.getInstance();

  private Vector<Event> eventTemplates = new Vector<Event>();

  private EventTemplateRegistry() { }

  public static EventTemplateRegistry get() {
    return EventTemplateRegistry.instance;
  }

  /**
   * Creates and stores an {@link Event} template built from a behavior. The behavior will also
   * have a {@link CodeObject} registered with the global {@link Recorder} instance.
   * @param behavior The behavior to create a template from
   * @return A behavior ordinal (an index to the event template)
   */
  public Integer register(CtBehavior behavior) {
    Event event = new Event(behavior);
    return this.register(event, behavior);
  }

  /**
   * Stores an event template built previously from a behavior. The behavior will also
   * have a {@link CodeObject} registered with the global {@link Recorder} instance.
   * @param event The {@link Event} template to be registered
   * @param behavior The behavior used to create the {@link Event} template
   * @returns The behavior ordinal (an index to the {@link Event} template)
   */
  public Integer register(Event event, CtBehavior behavior) {
    recorder.register(CodeObject.createTree(behavior));
    eventTemplates.add(event);
    return eventTemplates.size() - 1;
  }

  /**
   * Retrieve an {@link Event} template by ordinal.
   * @param templateId The behavior ordinal returned when the template was registered
   * @return An {@link Event} template if one exists at the given index. Otherwise, null.
   */
  public Event getTemplate(Integer templateId) {
    try {
      return eventTemplates.get(templateId);
    } catch (ArrayIndexOutOfBoundsException e) {
      // fall through
    }
    return null;
  }

  /**
   * Clones an {@link Event} template and sets the {@code event} field.
   * @param templateId The behavior ordinal
   * @param eventAction The value of the {@code event} field ({@code call}, {@code return}, etc.)
   * @return A copy of the event template with the {@code event} field set
   * @throws UnknownEventException If no template exists for the behavior ordinal given
   */
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
