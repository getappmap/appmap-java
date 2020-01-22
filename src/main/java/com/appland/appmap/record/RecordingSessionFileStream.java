package com.appland.appmap.record;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import com.alibaba.fastjson.JSONWriter;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class RecordingSessionFileStream extends RecordingSessionGeneric {
  private static final Integer MAX_EVENTS = 32;
  private static final String DEFAULT_FILENAME = "appmap.json";
  private static String OUTPUT_DIRECTORY = "./";

  private JSONWriter eventWriter;
  private String fileName = DEFAULT_FILENAME;

  static {
    String outputDirectory = System.getProperty("appmap.output.directory");
    if (outputDirectory != null) {
      OUTPUT_DIRECTORY = outputDirectory;
    }
  }

  public RecordingSessionFileStream(String scenarioName) {
    if (scenarioName.isEmpty()) {
      return;
    }

    this.fileName = String.format("%s.appmap.json", scenarioName);
  }

  private synchronized void flushEvents() {
    for (Event e : this.events) {
      this.eventWriter.writeObject(e);
    }

    this.events.clear();

    try {
      this.eventWriter.flush();
    } catch(IOException e) {
      throw new ActiveSessionException(
        String.format("failed to write:\n%s\n", e.getMessage())
      );
    }
  }

  @Override
  public synchronized void add(Event event) {
    super.add(event);

    if (this.events.size() >= MAX_EVENTS) {
      this.flushEvents();
    }
  }

  @Override
  public void start() {
    try {
      String filePath = Paths.get(OUTPUT_DIRECTORY, this.fileName).toString();
      this.eventWriter = new JSONWriter(new FileWriter(filePath));
    } catch(IOException e) {
      throw new ActiveSessionException(
        String.format("failed to start recording session:\n%s\n", e.getMessage())
      );
    }

    this.eventWriter.startObject();

    this.eventWriter.writeKey("version");
    this.eventWriter.writeValue("1.2");

    this.eventWriter.writeKey("metadata");
    this.eventWriter.writeValue("{}");

    this.eventWriter.writeKey("events");
    this.eventWriter.startArray();
  }

  @Override
  public synchronized String stop() {
    this.flushEvents();

    this.eventWriter.endArray();

    this.eventWriter.writeKey("classMap");
    this.eventWriter.writeValue(this.codeObjects.toArray());

    this.eventWriter.endObject();

    try {
      this.eventWriter.close();
    } catch(IOException e) {
      throw new ActiveSessionException(
        String.format("failed to finalize recording:\n%s\n", e.getMessage())
      );
    }

    System.err.printf("AppMap: wrote %s\n", this.fileName);

    return "";
  }
}