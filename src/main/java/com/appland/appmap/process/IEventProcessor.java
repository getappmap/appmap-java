package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;

interface IEventProcessor {
  Boolean processEvent(Event event, ThreadLock lock);
}
