package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookCondition;
import com.appland.appmap.transform.annotations.MethodEvent;

/**
 * Hooks to capture method exceptions from classes included in configuration.
 */
public class MethodException {
  private static final Recorder recorder = Recorder.INSTANCE;

  @ArgumentArray
  @HookCondition(value = ConfigCondition.class,  methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void handle(Event event, Object self, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
  }
}
