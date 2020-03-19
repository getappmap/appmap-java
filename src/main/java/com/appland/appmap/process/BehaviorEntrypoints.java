package com.appland.appmap.process;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.process.EventProcessorType;
import com.appland.appmap.record.EventAction;
import com.appland.appmap.record.EventFactory;
import com.appland.appmap.record.UnknownEventException;

import java.util.HashMap;
import javassist.CtBehavior;

public class BehaviorEntrypoints {
  private static EventFactory eventFactory = EventFactory.get();
  private static HashMap<EventProcessorType, Class<? extends IEventProcessor>> eventProcessors;

  static {
    ThreadProcessorStack processorStack = ThreadProcessorStack.current();
    processorStack.setLock(true); {
      eventProcessors = new HashMap<EventProcessorType, Class<? extends IEventProcessor>>();
      eventProcessors.put(EventProcessorType.Null, NullReceiver.class);
      eventProcessors.put(EventProcessorType.PassThrough, PassThroughReceiver.class);
      eventProcessors.put(EventProcessorType.HttpServlet, HttpServletReceiver.class);
      eventProcessors.put(EventProcessorType.SqlJdbc, SqlJdbcReceiver.class);
      eventProcessors.put(EventProcessorType.ServletFilter, ServletFilterReceiver.class);
      eventProcessors.put(EventProcessorType.ToggleRecord, ToggleRecordReceiver.class);
      eventProcessors.put(EventProcessorType.HttpRequest, HttpRequestReceiver.class);
    }
    processorStack.setLock(false);
  }

  private static IEventProcessor buildProcessor(EventProcessorType eventProcessorType) {
    Class<? extends IEventProcessor> processorClass =
        BehaviorEntrypoints.eventProcessors.get(eventProcessorType);

    if (processorClass == null) {
      return null;
    }

    IEventProcessor instance = null;

    try {
      instance = processorClass.newInstance();
    } catch(Exception e) {
      System.err.printf("AppMap: failed to create instance of %s\n", eventProcessorType);
      System.err.println(e.getClass() + ": " + e.getMessage());
      e.printStackTrace(System.err);
    }

    return instance;
  }

  public static boolean onEnter(Integer behaviorOrdinal,
                                        EventProcessorType eventProcessorType,
                                        Object selfValue,
                                        Object[] params) {
    ThreadProcessorStack processorStack = ThreadProcessorStack.current();
    if (processorStack.isLocked()) {
      // another method is blocking us from being processed
      return true;
    }

    Boolean continueMethodExecution = true;

    try {
      processorStack.setLock(true);

      Event event = BehaviorEntrypoints
          .eventFactory
          .create(behaviorOrdinal, EventAction.CALL)
          .setReceiver(selfValue);

      try {
        for (int i = 0; i < params.length; i++) {
          Value paramValue = event.parameters.get(i);
          paramValue.set(params[i]);
        }
      } catch (IndexOutOfBoundsException e) {
        System.err.println(e.getMessage());
      }

      IEventProcessor processor = BehaviorEntrypoints.buildProcessor(eventProcessorType);
      if (processor != null) {
        processorStack.push(processor);
        continueMethodExecution = processor.onEnter(event);
      }
    } catch (UnknownEventException e) {
      System.err.printf("AppMap: %s\n", e);
      return continueMethodExecution;
    } finally {
      processorStack.setLock(false);
    }

    return continueMethodExecution;
  }

  public static void onExit(Integer behaviorOrdinal,
                                    EventProcessorType eventProcessorType,
                                    Object returnValue) {
    ThreadProcessorStack processorStack = ThreadProcessorStack.current();
    if (processorStack.isLocked()) {
      // another method is blocking us from being processed
      return;
    }

    try {
      processorStack.setLock(true);

      Event event = BehaviorEntrypoints.eventFactory
          .create(behaviorOrdinal, EventAction.RETURN)
          .setReturnValue(returnValue);

      IEventProcessor eventProcessor = processorStack.pop();
      if (eventProcessor != null) {
        eventProcessor.onExit(event);
      }
    } finally {
      processorStack.setLock(false);
    }
  }

  public static Object boxValue(byte value) {
    return new Byte(value);
  }

  public static Object boxValue(char value) {
    return new Character(value);
  }

  public static Object boxValue(short value) {
    return new Short(value);
  }

  public static Object boxValue(long value) {
    return new Long(value);
  }

  public static Object boxValue(float value) {
    return new Float(value);
  }

  public static Object boxValue(double value) {
    return new Double(value);
  }

  public static Object boxValue(int value) {
    return new Integer(value);
  }

  public static Object boxValue(boolean value) { return new Boolean(value); }

  public static Object boxValue(Object value) {
    return value;
  }
}
