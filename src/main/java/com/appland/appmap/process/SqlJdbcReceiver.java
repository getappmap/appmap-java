package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.Recorder;

public class SqlJdbcReceiver implements IEventProcessor {
  private static final Recorder recorder = Recorder.getInstance();

  @Override
  public Boolean onEnter(Event event) {
    if (event.parameters == null || event.parameters.size() < 1) {
      return true;
    }

    Value sqlParam = event.parameters.get(0);
    event.setParameters(null);
    event.setSqlQuery(sqlParam.value.toString());

    recorder.add(event);
    return true;
  }

  @Override
  public void onExit(Event event) {
    recorder.add(event);
  }
}
