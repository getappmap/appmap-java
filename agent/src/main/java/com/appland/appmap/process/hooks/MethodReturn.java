package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.CallbackOn;
import com.appland.appmap.transform.annotations.HookCondition;
import com.appland.appmap.transform.annotations.MethodEvent;

/**
 * Hooks to capture method returns from classes included in configuration.
 */
public class MethodReturn {
  private static final Recorder recorder = Recorder.getInstance();

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookCondition(ConfigCondition.class)
  public static void handle(Event event, Object self, Object returnValue, Object[] args) {
    event.setReturnValue(returnValue);
    recorder.add(event);
  }

}
