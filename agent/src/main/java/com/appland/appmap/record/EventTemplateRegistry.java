package com.appland.appmap.record;

import java.util.ArrayList;
import java.util.List;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;

import javassist.CtBehavior;

/**
 * Stores events as templates built from behaviors intended to be hooked. Hooks can then access and
 * clone these templates before storing runtime information to be recorded. This caches event data at Class load time
 * rather than grabbing it through reflection every time a hook is invoked. It has benefits for DRY as well as
 * for performance.
 */
public class EventTemplateRegistry {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private static final EventTemplateRegistry instance = new EventTemplateRegistry();
  private static final Recorder recorder = Recorder.INSTANCE;

  private final List<Event> eventTemplates = new ArrayList<>();

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
  public Integer register(CtBehavior behavior, String[] labels) {
    Event event = new Event(behavior);
    return this.register(event, behavior, labels);
  }

  /**
   * Stores an event template built previously from a behavior. The behavior will also
   * have a {@link CodeObject} registered with the global {@link Recorder} instance.
   * @param event The {@link Event} template to be registered
   * @param behavior The behavior used to create the {@link Event} template
   * @return The behavior ordinal (an index to the {@link Event} template)
   */
  public synchronized Integer register(Event event, CtBehavior behavior, String[] labels) {
    recorder.registerCodeObject(CodeObject.createTree(behavior, labels));
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
    } catch (IndexOutOfBoundsException e) {
      // fall through
    }
    return null;
  }

  /**
   * Clones an {@link Event} template and sets the {@code event} field.
   * @param templateId The behavior ordinal
   * @return A copy of the event template with the {@code event} field set
   * @throws UnknownEventException If no template exists for the behavior ordinal given
   */
  public Event buildCallEvent(int templateId) {
    Event eventTemplate = lookupEventTemplate(templateId);
    Event event = Event.functionCallEvent(eventTemplate);
    for (Value param : eventTemplate.parameters) {
      event.addParameter(param);
    }
    return event;
  }

  /**
   * Prepares a return event.
   */
  public Event buildReturnEvent(int templateId) {
    Event eventTemplate = lookupEventTemplate(templateId);
    return Event.functionReturnEvent(eventTemplate);
  }

  Event lookupEventTemplate(int templateId) {
    try {
      return eventTemplates.get(templateId);
    } catch (IndexOutOfBoundsException e) {
      logger.warn(e,
          "unknown template for ordinal {} - have we been loaded by a non-system class loader?",
          templateId);

      throw new UnknownEventException("unknown template ordinal");
    }
  }
}
