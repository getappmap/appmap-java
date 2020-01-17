package com.appland.appmap.process;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.process.EventProcessorType;
import com.appland.appmap.record.EventAction;
import com.appland.appmap.record.EventFactory;
import com.appland.appmap.record.RuntimeRecorder;
import com.appland.appmap.record.UnknownEventException;

import java.util.HashMap;
import javassist.CtBehavior;

public class HookedBehavior {
  public static ThreadLock lock = new ThreadLock();
  private static EventFactory eventFactory = EventFactory.get();
  private static RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();
  private static HashMap<EventProcessorType, IEventProcessor> eventProcessors =
      new HashMap<>() {{
        put(EventProcessorType.Null, new NullReceiver());
        put(EventProcessorType.PassThrough, new PassThroughReceiver());
        put(EventProcessorType.HttpServlet, new HttpServletReceiver());
        put(EventProcessorType.SqlJdbc, new SqlJdbcReceiver());
        put(EventProcessorType.ServletFilter, new ServletFilterReceiver());
        put(EventProcessorType.ToggleRecord, new ToggleRecordReceiver());
      }};

  public static boolean onEnter(Integer behaviorOrdinal,
                                        EventProcessorType eventProcessor,
                                        Object selfValue,
                                        Object[] params) {
    if (MethodCallback.lock.tryLock()) {
      Event event = null;

      try {
        event = MethodCallback.eventFactory
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

      Boolean continueMethod = EventDispatcher.dispatchEvent(eventProcessor, event);

      MethodCallback.lock.releaseLock();

      EventDispatcher.runCallbacks();

      return continueMethod;
    }

    return true;
  }

  public static void onExit(Integer behaviorOrdinal,
                                    EventProcessorType eventProcessor,
                                    Object returnValue) {
    if (MethodCallback.lock.tryLock()) {
      Event event = MethodCallback.eventFactory
          .create(behaviorOrdinal, EventAction.RETURN)
          .setReturnValue(returnValue);

      EventDispatcher.dispatchEvent(eventProcessor, event);

      MethodCallback.lock.releaseLock();
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
