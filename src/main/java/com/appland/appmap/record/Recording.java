package com.appland.appmap.record;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Recording {
  private final File file;

  public Recording(File file) {
    this.file = file;
  }

  public void delete() {
    this.file.delete();
  }

  public void moveTo(String filePath) {
    System.out.println("Moving " + this.file.getPath() + " to " + filePath);

    try {
      Files.move(Paths.get(this.file.getPath()), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void readFully(boolean delete, Writer writer) throws IOException {
    final Reader reader = new FileReader(this.file);
    char[] buffer = new char[2048];
    int bytesRead;
    while ( (bytesRead = reader.read(buffer) ) != -1 ) {
      writer.write(buffer, 0, bytesRead);
    }
    writer.flush();

    if ( delete ) {
      this.delete();
    }
  }

  public int size() {
    return (int) this.file.length();
  }
}
