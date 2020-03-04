package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.CallbackOn;
import com.appland.appmap.transform.annotations.HookCondition;

public class MethodException {
  private static final Recorder recorder = Recorder.getInstance();

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @HookCondition(ConfigCondition.class)
  public static void handle(Event event, Object self, Object returnValue, Object[] args) {
    event.setReturnValue(returnValue);
    recorder.add(event);
  }

}