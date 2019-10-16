package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

public class PassThroughReceiver implements IEventProcessor {
  @Override
  public Event processEvent(Event event) {
    return event;
  }
}
