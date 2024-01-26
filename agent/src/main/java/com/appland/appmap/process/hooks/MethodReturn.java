package com.appland.appmap.process.hooks;

import java.lang.reflect.Method;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ThreadLock;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.record.EventTemplateRegistry;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookCondition;
import com.appland.appmap.transform.annotations.MethodEvent;

/**
 * Hooks to capture method returns from classes included in configuration.
 */
public class MethodReturn {
  private static final Recorder recorder = Recorder.getInstance();
  private static final EventTemplateRegistry templateRegistry = EventTemplateRegistry.get();

  @ArgumentArray
  @HookCondition(value = ConfigCondition.class,  methodEvent = MethodEvent.METHOD_RETURN)
  public static void handle(Event event, Object self, Object returnValue, Object[] args) {
    event.setReturnValue(returnValue);
    recorder.add(event);
  }

  public static void onReturn(int returnOrdinal, int excOrdinal, Object receiver, Method method,
      Object[] args,
      Object ret,
      Throwable exc) throws Throwable {
    try {
      if (exc == null) {
        handleReturn(returnOrdinal, receiver, method, args, ret);
      } else {
        handleExc(excOrdinal, receiver, method, args, exc);
      }
    } finally {
      ThreadLock.current().exit();
    }
  }

  static void handleReturn(int returnOrdinal, Object self, Method method, Object[] args,
      Object ret) throws Throwable {
    if (ThreadLock.current().lock()) {
      handle(templateRegistry.buildReturnEvent(returnOrdinal),
          self, ret, args);
      ThreadLock.current().unlock();
    }
  }

  static void handleExc(int excOrdinal, Object self, Method method, Object[] args,
      Throwable exc) throws Throwable {
    if (ThreadLock.current().lock()) {
      MethodException.handle(templateRegistry.buildReturnEvent(excOrdinal),
          self, exc, args);
      ThreadLock.current().unlock();
    }
    throw exc;
  }
}
