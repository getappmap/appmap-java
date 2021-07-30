package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.util.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;

public class RecordingSession {

  private final HashSet<String> classReferences = new HashSet<>();
  private boolean eventReceived = false;
  private Path tmpPath;
  private AppMapSerializer serializer;
  private Recorder.Metadata metadata;

  RecordingSession() {
    this.tmpPath = null;
  }

  void start(Recorder.Metadata metadata) {
    if (this.serializer != null) {
      throw new IllegalStateException("AppMap: Unable to start a recording, because a recording is already in progress");
    }

    this.metadata = metadata;

    try {
      this.tmpPath = Files.createTempFile(null, ".appmap.json");
      this.serializer = AppMapSerializer.open(new FileWriter(this.tmpPath.toFile()));
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

  public Recorder.Metadata getMetadata() {
    return this.metadata;
  }

  public synchronized void add(Event event) {
    this.eventReceived = true;
    if ( event.event.equals("call") ) {
      // Events may refer to non-code objects such as SQL queries, in that case we don't
      // need to worry about tracking class references.
      if ( event.definedClass != null && event.methodId != null ) {
        String key = event.definedClass +
            ":" + event.methodId +
            ":" + event.isStatic +
            ":" + event.lineNumber;
        this.classReferences.add(key);
      }
    }

    try {
      this.serializer.writeEvents(Collections.singletonList(event));
    } catch (IOException e) {
      throw new ActiveSessionException(String.format("Failed to flush recording session:\n%s\n", e.getMessage()), e);
    }
  }

  public synchronized Recording checkpoint() {
    if (this.serializer == null) {
      throw new IllegalStateException("AppMap: Unable to checkpoint the recording because no recording is in progress.");
    }

    Path targetPath;
    try {
      this.serializer.flush();

      targetPath = Files.createTempFile(null, ".appmap.json");
      Files.copy(this.tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

      // Creating AppMapSerializer will re-write the "begin object" token: '{'.
      // By using RandomAccessFile we can erase that character.
      // If we don't let the JSON writer write the "begin object" token, it refuses
      // to do anything else properly either.
      RandomAccessFile raf = new RandomAccessFile(targetPath.toFile(), "rw");
      Writer fw = new OutputStreamWriter(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          raf.write(b);
        }
      });
      raf.seek(targetPath.toFile().length());

      if (  eventReceived ) {
        fw.write("],");
      }
      fw.flush();

      AppMapSerializer serializer = AppMapSerializer.reopen(fw, raf);
      serializer.writeClassMap(this.getClassMap());
      serializer.writeMetadata(this.metadata);
      serializer.finish();
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
      this.serializer.writeClassMap(this.getClassMap());
      this.serializer.writeMetadata(this.metadata);
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

  CodeObjectTree getClassMap() {
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
