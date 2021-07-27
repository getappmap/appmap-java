package com.appland.appmap.record;

import com.alibaba.fastjson.JSONWriter;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class RecordingSession {
  public static class Metadata {
    public String scenarioName;
    public String recorderName;
    public String framework;
    public String frameworkVersion;
    public String recordedClassName;
    public String recordedMethodName;
    public String feature;
    public String featureGroup;
  }

  static class EventList {
    private static final Integer MAX_BUFFER_SIZE = 32;

    private final Vector<Event> events = new Vector<Event>();
    private final HashSet<String> classReferences = new HashSet<>();

    public synchronized List<Event> add(Event event) {
      this.events.add(event);

      String key = event.definedClass +
          ":" + event.methodId +
          ":" + event.isStatic +
          ":" + event.lineNumber;
      this.classReferences.add(key);

      if (this.events.size() <= MAX_BUFFER_SIZE) {
        return Collections.emptyList();
      }

      return flush();
    }

    public synchronized List<Event> flush() {
      final List<Event> result = events.stream().collect(Collectors.toList());
      this.events.clear();
      return result;
    }

    protected CodeObjectTree getClassMap() {
      CodeObjectTree registeredObjects = Recorder.getInstance().getRegisteredObjects();
      CodeObjectTree classMap = new CodeObjectTree();
      for (String key : this.classReferences) {
        String[] parts = key.split(":");

        CodeObject methodBranch = registeredObjects.getMethodBranch(parts[0], parts[1], Boolean.valueOf(parts[2]), Integer.valueOf(parts[3]));
        if (methodBranch != null)
          classMap.add(methodBranch);
      }

      return classMap;
    }
  }

  private final EventList eventList = new EventList();

  private Path tmpPath;
  private AppMapSerializer serializer;

  /**
   * Constructor. You typically shouldn't be creating this outside of the {@link Recorder}.
   */
  public RecordingSession() {
    this.tmpPath = null;
  }

  public synchronized void start(Metadata metadata) {
    if (this.serializer != null) {
      throw new IllegalStateException("AppMap: Unable to start a recording, because a recording is already in progress");
    }

    try {
      this.tmpPath = Files.createTempFile(null, ".appmap.json");
      this.serializer = AppMapSerializer.open(new FileWriter(this.tmpPath.toFile()));
      this.serializer.writeMetadata(metadata);
    } catch (IOException e) {
      this.tmpPath = null;
      this.serializer = null;
      throw new RuntimeException(e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (RecordingSession.this.tmpPath != null) {
        RecordingSession.this.tmpPath.toFile().delete();
        RecordingSession.this.tmpPath = null;
      }
    }));
  }

  public synchronized void add(Event event) {
    try {
      this.serializer.writeEvents(this.eventList.add(event));
    } catch (IOException e) {
      throw new ActiveSessionException(String.format("Failed to flush recording session:\n%s\n", e.getMessage()), e);
    }
  }

  public synchronized Recording checkpoint() {
    if (this.serializer == null) {
      throw new IllegalStateException("AppMap: Unable to checkpoint the recording because no recording is in progress.");
    }

    // Flush events
    // Close the writer and copy the temp file to a new file
    // Write the class map
    // Reopen the event stream on the temp file

    Path targetPath;
    try {
      this.serializer.writeEvents(this.eventList.flush());
      this.serializer.flush();

      targetPath = Files.createTempFile(null, ".appmap.json");
      Files.copy(this.tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

      FileWriter fw = new FileWriter(targetPath.toFile(), true);
      fw.write("],\"classMap\":");
      JSONWriter jw = new JSONWriter(fw);
      jw.writeObject(this.eventList.getClassMap().toArray());
      jw.flush();
      fw.write('}');
      fw.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Logger.printf("AppMap: Recording flushed at checkpoint\n");
    Logger.printf("AppMap: Wrote recording to file %s\n", targetPath);

    return new Recording(targetPath.toFile());
  }

  public synchronized Recording stop() {
    if (this.serializer == null) {
      throw new IllegalStateException("AppMap: Unable to stop the recording because no recording is in progress.");
    }

    try {
      this.serializer.writeEvents(this.eventList.flush());
      this.serializer.writeClassMap(this.eventList.getClassMap());
      this.serializer.finish();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    File file = this.tmpPath.toFile();
    this.serializer = null;
    this.tmpPath = null;

    Logger.printf("AppMap: Recording finished\n");
    Logger.printf("AppMap: Wrote recording to file %s\n", file.getPath());

    return new Recording(file);
  }
}
