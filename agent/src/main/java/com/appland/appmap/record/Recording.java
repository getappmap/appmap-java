package com.appland.appmap.record;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.appland.appmap.util.Logger;

public class Recording {
    private final File file;

    public Recording(File file) {
        this.file = file;
    }

    public void delete() {
        this.file.delete();
    }

    public void moveTo(String filePath) {
        Path sourcePath = Paths.get(this.file.getPath());
        Path targetPath = Paths.get(filePath);

        Logger.printf("Moving %s to %s\n", sourcePath, targetPath);

        Function<FileMover, FileMover.Result> tryMove = mover -> {
            IOException exception = null;
            try {
                mover.move();
            } catch (IOException e) {
                exception = e;
            }
            return new FileMover.Result(exception);
        };

        FileMover[] movers = new FileMover[]{
            () -> Files.move(sourcePath, targetPath, REPLACE_EXISTING, ATOMIC_MOVE),
            () -> Files.move(sourcePath, targetPath, REPLACE_EXISTING),
            () -> {
                Files.copy(sourcePath, targetPath, REPLACE_EXISTING);
                sourcePath.toFile().delete();
            },
        };
        List<String> errors = new ArrayList<>();
        for (FileMover mover: movers) {
            FileMover.Result r = tryMove.apply(mover);
            if ( r.isSucceeded() ) {
                errors.clear();;
                break;
            }
            errors.add(r.exception.getMessage());
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join(", ", errors));
        }
    }

    public void readFully(boolean delete, Writer writer) throws IOException {
      try (final Reader reader = new FileReader(this.file)) {
        char[] buffer = new char[2048];
        int bytesRead;
        while ((bytesRead = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, bytesRead);
        }
        writer.flush();

        if (delete) {
          this.delete();
        }
      }
    }

    public int size() {
        return (int) this.file.length();
    }

  public InputStream asInputStream() throws IOException {
    return new FileInputStream(this.file);
  }

    interface FileMover {
        class Result {

            final IOException exception;

            Result(IOException exception) {
                this.exception = exception;
            }

            boolean isSucceeded() {
                return this.exception == null;
            }
        }

        void move() throws IOException;
    }
}
