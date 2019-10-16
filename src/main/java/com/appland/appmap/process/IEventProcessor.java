package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

interface IEventProcessor {
  Event processEvent(Event event);
}
