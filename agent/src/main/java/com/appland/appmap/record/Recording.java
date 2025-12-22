package com.appland.appmap.record;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.appland.appmap.util.Logger;
import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;

public class Recording {
    private static final TaggedLogger logger = AppMapConfig.getLogger(null);

    private final Path outputDirectory;
    private final File file;

    public static final DateTimeFormatter RECORDING_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public Recording(String recorderName, File file) {
        this.outputDirectory = Properties.getOutputDirectory().resolve(recorderName);
        this.file = file;
    }

    public void delete() {
        this.file.delete();
    }

    public Path moveTo(String filePath) {
        return moveTo(outputDirectory.resolve(filePath));
    }

    public Path moveTo(Path targetPath) {
        Path sourcePath = Paths.get(this.file.getPath());

        logger.info("Saving AppMap {}", targetPath::toAbsolutePath);
        logger.debug("Moving {}", sourcePath::toString, targetPath::toAbsolutePath);

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
        IOException lastException = null;
        try {
            Files.createDirectories(outputDirectory);

            for (FileMover mover : movers) {
                FileMover.Result r = tryMove.apply(mover);
                if (r.isSucceeded()) {
                    errors.clear();
                    break;
                }
                lastException = r.exception;
                errors.add(r.exception.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            errors.add(e.toString());
        }

        if (!errors.isEmpty()) {
            if (lastException != null) {
                lastException.printStackTrace();
            }
            throw new RuntimeException(String.join(", ", errors));
        }

        Logger.printUserMessage("AppMap recording saved to %s\n", targetPath.toAbsolutePath());
        return targetPath;
    }

    public void readFully(boolean delete, Writer writer) throws IOException {
      try (final Reader reader = new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8)) {
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
