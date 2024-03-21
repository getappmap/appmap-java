package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.process.ThreadLock;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.record.EventTemplateRegistry;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookCondition;

/**
 * Hooks to capture method invocations from classes included in configuration.
 */
public class MethodCall {
  private static final Recorder recorder = Recorder.INSTANCE;

  @ArgumentArray
  @HookCondition(ConfigCondition.class)
  public static void handle(Event event, Object self, Object[] args) {
    int argsLen = args != null ? args.length : 0;
    for (int i = 0; i < argsLen; i++) {
      Value param = event.parameters.get(i);
      param.set(args[i]);
    }

    event.setReceiver(self);

    recorder.add(event);
  }

  public static void onCall(int callOrdinal, Object receiver, Object[] args) {

    EventTemplateRegistry etr = EventTemplateRegistry.get();

    ThreadLock.current().enter();

    if (ThreadLock.current().lock()) {
      handle(etr.buildCallEvent(callOrdinal), receiver,
          args);
      ThreadLock.current().unlock();
    } ;

  }
}
