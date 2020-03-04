package com.appland.appmap.record;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Vector;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class RecordingSessionMemory extends RecordingSessionGeneric {
  // protected Vector<Event> events = new Vector<Event>();
  // protected CodeObjectTree codeObjects = new CodeObjectTree();
  private final Metadata metadata;

  public RecordingSessionMemory(Metadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public void start() {
    // do nothing
  }

  @Override
  public String stop() {
    StringWriter stringWriter = new StringWriter();
    AppMapSerializer serializer = new AppMapSerializer(stringWriter);
    String json;

    try {
      serializer.write(this.metadata);
      serializer.write(this.events);
      serializer.write(this.codeObjects);
      serializer.finalize();

      json = stringWriter.toString();
      stringWriter.close();
    } catch (IOException e) {
      throw new ActiveSessionException(
        String.format("failed to finalize recording:\n%s\n", e.getMessage())
      );
    }

    return json;
  }
}