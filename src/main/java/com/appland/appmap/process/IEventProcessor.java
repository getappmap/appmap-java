package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

interface IEventProcessor {
  Boolean onEnter(Event event);

  void onExit(Event event);
}
