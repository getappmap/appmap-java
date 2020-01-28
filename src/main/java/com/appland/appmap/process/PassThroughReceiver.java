package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;

public class PassThroughReceiver implements IEventProcessor {
  private static final Recorder recorder = Recorder.getInstance();

  @Override
  public Boolean processEvent(Event event) {
    recorder.add(event);
    return true;
  }
}
