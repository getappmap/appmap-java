package com.appland.appmap.cli;

import picocli.CommandLine;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "init", description = "Suggests AppMap configuration settings for a new project.")
public class Init implements Callable<Integer> {
  @CommandLine.ParentCommand
  private CLI parent;

  public Integer call() throws Exception {
    System.err.printf("Init AppMap project configuration in directory: %s", parent.directory);
    return 0;
  }
}
