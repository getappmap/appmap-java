package com.appland.appmap.cli;

import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CommandLine.Command()
public class CLI {
  private static final Object STDOUT_CONST = new Object();

  @CommandLine.Option(names = { "-d", "--directory" }, description = "Project directory", required = true)
  public String directory;

  @CommandLine.Option(names = { "-o", "--output-file" }, description = "File to receive the output. Default: stdout")
  public Object outputFileName = STDOUT_CONST;

  Map<Object, PrintStream> streams = new ConcurrentHashMap<>();

  {
    streams.put(STDOUT_CONST, System.out);
  }

  public PrintStream getOutputStream() {
    return streams.computeIfAbsent(this.outputFileName, (fileName) -> {
      System.err.printf("Directing command output to: %s\n", this.outputFileName.toString());
      try {
        return new PrintStream(new FileOutputStream(this.outputFileName.toString()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CLI())
        .addSubcommand("init", Init.class)
        .addSubcommand("status", Status.class)
        .addSubcommand("validate", Validate.class)
        .execute(args);
    System.exit(exitCode);
  }

  public static String projectName(File directory) {
    try {
      return directory.getCanonicalFile().getName();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
