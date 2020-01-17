package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public interface IEventRecorder {
  public void recordEvent(Event event);
  public void recordCodeObject(CodeObject codeObject);
  public void close();
}
