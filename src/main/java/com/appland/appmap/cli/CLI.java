package com.appland.appmap.cli;

import picocli.CommandLine;

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
}
