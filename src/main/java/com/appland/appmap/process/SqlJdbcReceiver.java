package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;

public class SqlJdbcReceiver implements IEventProcessor {
  @Override
  public Event processEvent(Event event) {
    if (event.event.equals("return")) {
      return event;
    }

    if (event.parameters == null) {
      return null;
    }

    if (event.parameters.size() < 1) {
      return null;
    }

    Value sqlParam = event.parameters.get(0);
    event.setParameters(null);
    event.setSqlQuery(sqlParam.value.toString());
    System.out.println(sqlParam.value.toString());

    return event;
  }
}
