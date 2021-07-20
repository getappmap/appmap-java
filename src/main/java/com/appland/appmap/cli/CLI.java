package com.appland.appmap.cli;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

@CommandLine.Command()
public class CLI {
  @CommandLine.Option(names = { "-d", "--directory" }, description = "Project directory", required = true)
  public String directory;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CLI())
        .addSubcommand("status", Status.class)
        .addSubcommand("init", Init.class)
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
