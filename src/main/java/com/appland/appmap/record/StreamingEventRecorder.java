package com.appland.appmap.record;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONWriter;
import com.appland.appmap.output.v1.AppMap;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Vector;

/**
 * StreamingRecorder is responsible for recording the classMap and runtime events.
 */
public class StreamingEventRecorder implements IEventRecorder {
  private String fileName;
  private JSONWriter eventWriter;

  public StreamingRecorder(String scenarioName) {
    this.fileName = String.format("%s.appmap.json", scenarioName);
    this.eventWriter = new JSONWriter(new FileWriter(this.fileName));
    this.eventWriter.startArray();
  }

  public void recordEvent(Event event) {

  }

  public void open();
  public void close();
}
