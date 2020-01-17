package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public interface IRecordingSession {
  public void add(Event event) throws ActiveSessionException;
  public void add(CodeObject codeObject) throws ActiveSessionException;
  public void start() throws ActiveSessionException;
  public String stop() throws ActiveSessionException;
}