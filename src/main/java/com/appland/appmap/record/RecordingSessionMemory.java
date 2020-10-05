package com.appland.appmap.record;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Receives recording data and writes it to a buffer in memory.
 */
public class RecordingSessionMemory extends RecordingSessionGeneric {
  private final Metadata metadata;

  /**
   * Constructor. You typically shouldn't be creating this outside of the {@link Recorder}.
   * @param metadata Recording metadata
   */
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
      serializer.write(getClassMap());
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
