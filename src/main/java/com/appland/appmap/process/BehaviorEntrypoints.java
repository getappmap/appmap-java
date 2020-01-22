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
  public static ThreadLock lock = new ThreadLock();
  private static EventFactory eventFactory = EventFactory.get();
  private static final HashMap<EventProcessorType, IEventProcessor> eventProcessors;

  static {
    lock.tryLock();
    eventProcessors = new HashMap<EventProcessorType, IEventProcessor>();
    eventProcessors.put(EventProcessorType.Null, new NullReceiver());
    eventProcessors.put(EventProcessorType.PassThrough, new PassThroughReceiver());
    eventProcessors.put(EventProcessorType.HttpServlet, new HttpServletReceiver());
    eventProcessors.put(EventProcessorType.SqlJdbc, new SqlJdbcReceiver());
    eventProcessors.put(EventProcessorType.ServletFilter, new ServletFilterReceiver());
    eventProcessors.put(EventProcessorType.ToggleRecord, new ToggleRecordReceiver());
    lock.releaseLock();
  }

  private static Boolean processEvent(Event event, EventProcessorType eventProcessorType) {
    IEventProcessor eventProcessor = BehaviorEntrypoints.eventProcessors.get(eventProcessorType);
    if (eventProcessor == null) {
      return true;
    }

    return eventProcessor.processEvent(event, BehaviorEntrypoints.lock);
  }

  public static boolean onEnter(Integer behaviorOrdinal,
                                        EventProcessorType eventProcessor,
                                        Object selfValue,
                                        Object[] params) {
    if (BehaviorEntrypoints.lock.tryLock()) {
      Event event = null;

      try {
        event = BehaviorEntrypoints
          .eventFactory
          .create(behaviorOrdinal, EventAction.CALL)
          .setReceiver(selfValue);
      } catch (UnknownEventException e) {
        System.err.printf("AppMap: %s\n", e);
        return true;
      }

      try {
        for (int i = 0; i < params.length; i++) {
          Value paramValue = event.parameters.get(i);
          paramValue.set(params[i]);
        }
      } catch (IndexOutOfBoundsException e) {
        System.err.println(e.getMessage());
      }

      Boolean continueBehaviorExecution = BehaviorEntrypoints.processEvent(event, eventProcessor);

      BehaviorEntrypoints.lock.releaseLock();

      return continueBehaviorExecution;
    }

    return true;
  }

  public static void onExit(Integer behaviorOrdinal,
                                    EventProcessorType eventProcessor,
                                    Object returnValue) {
    if (BehaviorEntrypoints.lock.tryLock()) {
      Event event = BehaviorEntrypoints.eventFactory
          .create(behaviorOrdinal, EventAction.RETURN)
          .setReturnValue(returnValue);

      BehaviorEntrypoints.processEvent(event, eventProcessor);

      BehaviorEntrypoints.lock.releaseLock();
    }
  }

  public static void lockThread() {
    BehaviorEntrypoints.lock.tryLock();
  }

  public static void releaseThread() {
    BehaviorEntrypoints.lock.releaseLock();
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
