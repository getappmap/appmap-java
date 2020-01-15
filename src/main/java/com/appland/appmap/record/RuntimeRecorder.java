package com.appland.appmap.record;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.output.v1.AppMap;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Vector;

/**
 * RuntimeRecorder is responsible for recording the classMap and runtime events.
 */
// TODO: this class needs to be restricted to a single thread or group of threads with a common parent.
public class RuntimeRecorder {
  private static RuntimeRecorder instance = new RuntimeRecorder();
  private final static String DEFAULT_OUTPUT_DIRECTORY = "./";

  private Boolean recording = false;
  private String recordingName = "appmap.json";
  private String outputDirectory = DEFAULT_OUTPUT_DIRECTORY;

  private CodeObjectTree classMap = new CodeObjectTree();
  private Vector<Event> events = new Vector<Event>();
  private Object mutex = new Object();

  private RuntimeRecorder() {
    String outputDirectory = System.getProperty("appmap.output.directory");
    if (outputDirectory != null) {
      this.outputDirectory = outputDirectory;
    }
  }

  public static RuntimeRecorder get() {
    return RuntimeRecorder.instance;
  }

  public void recordEvent(Event event) {
    if (!isRecording()) {
      return;
    }

    synchronized (this.mutex) {
      this.events.add(event.freeze());
    }
  }

  public void recordCodeObject(CodeObject codeObject) {
    if (!isRecording()) {
      return;
    }

    synchronized (this.mutex) {
      this.classMap.add(codeObject);
    }
  }

  public Boolean isEmpty() {
    synchronized (this.mutex) {
      return this.events.isEmpty() && this.classMap.isEmpty();
    }
  }

  public String flushJson() {
    AppMap appMap = new AppMap();

    synchronized (this.mutex) {
      appMap.classMap = classMap.toArray();
      appMap.events = new Event[this.events.size()];
      this.events.copyInto(appMap.events);
      this.events.clear();
    }

    return JSON.toJSONString(appMap);
  }

  public void flushToFile(String filename) {
    String finalDestination = Paths.get(this.outputDirectory, filename).toString();
    String json = this.flushJson();
    System.err.printf("writing data to %s... ", finalDestination);

    try {
      PrintWriter out = new PrintWriter(finalDestination);
      out.print(json);
      out.close();

      System.err.print("done.\n");
    } catch (FileNotFoundException e) {
      System.err.printf("failed: %s\n", e.getMessage());
    } catch (Exception e) {
      System.err.printf("failed: %s\n", e.getMessage());
    }
  }

  public void flushToFile() {
    String filename = String.format("%s.appmap.json", this.recordingName);
    this.flushToFile(filename);
  }

  public void setRecording(Boolean recording) {
    synchronized (this.recording) {
      this.recording = recording;
    }
  }

  public void setRecordingName(String recordingName) {
    // this isn't synchronized because this entire object needs to be locked to a single thread
    this.recordingName = recordingName;
  }

  public Boolean isRecording() {
    synchronized (this.recording) {
      return this.recording;
    }
  }
}
