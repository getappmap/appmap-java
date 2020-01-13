package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.EventDispatcher;
import com.appland.appmap.record.RuntimeRecorder;


public class ToggleRecordReceiver implements IEventProcessor {
  private static final RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();

  @Override
  public int processEvent(Event event) {
    Boolean isEnteringMethod = event.event.equals("call");
    if (isEnteringMethod) {
      runtimeRecorder.setRecording(true);
      runtimeRecorder.setRecordingName(event.methodId);
    } else {
      runtimeRecorder.setRecording(false);
      if (!runtimeRecorder.isEmpty()) {
        runtimeRecorder.flushToFile();
      }
    }

    return EventDispatcher.EVENT_DISCARD;
  }
}
