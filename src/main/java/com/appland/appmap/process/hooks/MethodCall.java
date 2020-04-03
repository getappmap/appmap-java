package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookCondition;

/**
 * Hooks to capture method invocations from classes included in configuration.
 */
public class MethodCall {
  private static final Recorder recorder = Recorder.getInstance();

  @ArgumentArray
  @HookCondition(ConfigCondition.class)
  public static void handle(Event event, Object self, Object[] args) {
    for (int i = 0; i < args.length; i++) {
      Value param = event.parameters.get(i);
      param.set(args[i]);
    }

    event.setReceiver(self);

    recorder.add(event);
  }

}
