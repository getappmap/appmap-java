package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

public class NullReceiver implements IEventProcessor {
  @Override
  public Event processEvent(Event event) {
    return null;
  }
}
