package com.appland.appmap.record;

import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.util.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Receives recording data and writes it to disk.
 */
public class RecordingSessionFileStream extends RecordingSessionGeneric {
  private static final Integer MAX_EVENTS = 32;
  private static final Integer FILENAME_MAX_LENGTH = 255;
  private static final String DEFAULT_FILENAME = "appmap.json";
  private static final String FILE_EXTENSION  = "appmap.json";

  private FileWriter fileWriter;
  private final Metadata metadata;
  private String fileName = DEFAULT_FILENAME;
  private AppMapSerializer serializer;

  /**
   * Constructor. You typically shouldn't be creating this outside of the {@link Recorder}.
   * @param fileName Output file name
   * @param metadata Recording metadata
   */
  public RecordingSessionFileStream(String fileName, Metadata metadata) {
    this.metadata = metadata;
    if (fileName != null && !fileName.trim().isEmpty()) {
      if (fileName.length() + DEFAULT_FILENAME.length() >=  FILENAME_MAX_LENGTH) {
        try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(fileName.getBytes(StandardCharsets.UTF_8));
        String nameDigest = Base64.getUrlEncoder().encodeToString(md.digest());

        this.fileName = String.format(
                "%s." + FILE_EXTENSION,
                fileName.substring(0, FILENAME_MAX_LENGTH - nameDigest.length() - FILE_EXTENSION.length() - 1)
                        + nameDigest);
        } catch (NoSuchAlgorithmException e) { }
      } else {
        this.fileName = String.format("%s." + DEFAULT_FILENAME, fileName);
      }
    }
  }

  private synchronized void flushEvents() {
    try {
      this.serializer.write(this.events);
    } catch (IOException e) {
      throw new ActiveSessionException(
        String.format("failed to start recording session:\n%s\n", e.getMessage())
      );
    }
    
    this.events.clear();
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
      String filePath = Paths.get(Properties.OutputDirectory, this.fileName).toString();
      this.fileWriter = new FileWriter(filePath);
      this.serializer = new AppMapSerializer(this.fileWriter);
    } catch (IOException e) {
      throw new ActiveSessionException(
        String.format("failed to start recording session:\n%s\n", e.getMessage())
      );
    }

    try {
      this.serializer.write(this.metadata);
    } catch (IOException e) {
      throw new ActiveSessionException(
        String.format("failed to start recording session:\n%s\n", e.getMessage())
      );
    }
  }

  @Override
  public synchronized String stop() {
    if (this.serializer == null) {
      return "";
    }

    this.flushEvents();

    try {
      this.serializer.write(getClassMap());
      this.serializer.finalize();
      this.fileWriter.close();
    } catch (IOException e) {
      throw new ActiveSessionException(
        String.format("failed to finalize recording:\n%s\n", e.getMessage())
      );
    }

    Logger.printf("wrote %s\n", this.fileName);

    return "";
  }
}
