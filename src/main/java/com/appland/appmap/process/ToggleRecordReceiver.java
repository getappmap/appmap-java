package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;


public class ToggleRecordReceiver implements IEventProcessor {
  private static final Recorder recorder = Recorder.getInstance();

  @Override
  public Boolean processEvent(Event event, ThreadLock lock) {
    try {
      if (event.event.equals("call")) {
        recorder.start(event.methodId);
      } else {
        recorder.stop();
      }
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: %s\n", e.getMessage());
    }

    return true;
  }
}
