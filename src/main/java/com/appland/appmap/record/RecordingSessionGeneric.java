package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

import java.util.Vector;

public class RecordingSessionGeneric implements IRecordingSession {
  protected Vector<Event> events = new Vector<Event>();
  protected CodeObjectTree codeObjects = new CodeObjectTree();

  public void add(Event event) {
    this.events.add(event);
  }

  public void add(CodeObject codeObject) {
    this.codeObjects.add(codeObject);
  }

  public void start() {
    throw new UnsupportedOperationException();
  }

  public String stop() {
    throw new UnsupportedOperationException();
  }
}
