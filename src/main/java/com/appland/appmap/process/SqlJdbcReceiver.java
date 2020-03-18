package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.Recorder;

public class SqlJdbcReceiver extends EventProcessorLock {
  private static final Recorder recorder = Recorder.getInstance();

  @Override
  protected String getLockKey() {
    return "sql_query";
  }

  @Override
  public Boolean onEnterLock(Event event) {
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
  public void onExitLock(Event event) {
    recorder.add(event);
  }
}
