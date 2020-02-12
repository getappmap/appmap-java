package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

public class NullReceiver implements IEventProcessor {
  @Override
  public Boolean onEnter(Event event) {
    return true;
  }

  @Override
  public void onExit(Event event) { }
}
