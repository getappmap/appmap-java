package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

public class NullReceiver implements IEventProcessor {
  @Override
  public Boolean processEvent(Event event, ThreadLock lock) {
    return true;
  }
}
