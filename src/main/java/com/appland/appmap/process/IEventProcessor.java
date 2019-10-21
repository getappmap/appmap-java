package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

interface IEventProcessor {
  int processEvent(Event event);
}
