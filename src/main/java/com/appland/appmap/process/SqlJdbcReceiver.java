package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;

public class SqlJdbcReceiver implements IEventProcessor {
  @Override
  public int processEvent(Event event) {
    if (event.event.equals("return")) {
      return EventDispatcher.EVENT_RECORD;
    }

    if (event.parameters == null) {
      return EventDispatcher.EVENT_DISCARD;
    }

    if (event.parameters.size() < 1) {
      return EventDispatcher.EVENT_DISCARD;
    }

    Value sqlParam = event.parameters.get(0);
    event.setParameters(null);
    event.setSqlQuery(sqlParam.value.toString());
    System.out.println(sqlParam.value.toString());

    return EventDispatcher.EVENT_RECORD;
  }
}
