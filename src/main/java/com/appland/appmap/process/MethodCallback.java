package com.appland.appmap.process;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.process.EventProcessorType;
import com.appland.appmap.record.EventAction;
import com.appland.appmap.record.EventFactory;
import com.appland.appmap.record.RuntimeRecorder;
import java.util.HashMap;
import javassist.CtBehavior;

public class MethodCallback {
  public static ThreadLock lock = new ThreadLock();
  private static EventFactory eventFactory = EventFactory.get();
  private static RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();

  public static void onBehaviorTransformed(CtBehavior behavior) {
    CodeObject rootObject = CodeObject.createTree(behavior);
    MethodCallback.runtimeRecorder.recordCodeObject(rootObject);
  }

  public static boolean onMethodInvocation(Integer behaviorOrdinal,
                                        EventProcessorType eventProcessor,
                                        Object selfValue,
                                        Object[] params) {
    if (MethodCallback.lock.tryLock()) {
      Event event = MethodCallback.eventFactory
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

      Boolean continueMethod = EventDispatcher.dispatchEvent(eventProcessor, event);

      MethodCallback.lock.releaseLock();

      return continueMethod;
    }

    return true;
  }

  public static void onMethodReturn(Integer behaviorOrdinal,
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

  public static Object boxValue(boolean value) {
    return new Boolean(value);
  }

  public static Object boxValue(Object value) {
    return value;
  }
}
