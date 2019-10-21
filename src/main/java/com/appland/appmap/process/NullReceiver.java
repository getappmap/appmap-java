package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

public class NullReceiver implements IEventProcessor {
  @Override
  public int processEvent(Event event) {
    return EventDispatcher.EVENT_DISCARD;
  }
}
