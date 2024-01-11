package com.appland.appmap.util.tinylog;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.path.DynamicPath;
import org.tinylog.writers.FileWriter;
import org.tinylog.writers.Writer;

import com.appland.appmap.config.Properties;

public class AppMapFileWriter implements Writer {

  private FileWriter impl;

  public AppMapFileWriter(Map<String, String> properties) {
    try {
      String filePattern = properties.get("file");
      DynamicPath path = new DynamicPath(filePattern);
      String filename = path.resolve();
      properties.put("file", filename);
      impl = new FileWriter(properties);
      if (Properties.DisableLogFile != Boolean.TRUE) {
        System.err.println(String
            .format("AppMap Java Agent log file: %s", Paths.get(filename).toAbsolutePath()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Collection<LogEntryValue> getRequiredLogEntryValues() {
    return impl.getRequiredLogEntryValues();
  }

  @Override
  public void write(LogEntry logEntry) throws Exception {
    try {
      impl.write(logEntry);
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  public void flush() throws Exception {
    impl.flush();
  }

  @Override
  public void close() throws Exception {
    impl.close();
  }

}
