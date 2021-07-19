package com.appland.appmap.cli;

import picocli.CommandLine;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "status", description = "Prints AppMap status of the Java project in a specified directory.")
public class Status implements Callable<Integer> {
  @CommandLine.ParentCommand
  private CLI parent;

  public Integer call() throws Exception {
    System.err.printf("Reporting AppMap project status in directory: %s", parent.directory);
    return 0;
  }
}
