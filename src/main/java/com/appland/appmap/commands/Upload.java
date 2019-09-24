package com.appland.appmap.commands;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "upload",
    description = "Upload a scenario file to AppLand")
public class Upload implements Callable<Void> {
  @Parameters(description = "Name of the file to upload")
  private File filename;

  @Option(names = { "--open" },
      negatable = true,
      description = "Whether to open the new scenario in the browser (default: ${DEFAULT-VALUE})")
  private Boolean open = true;

  @Option(names = { "--owner" },
      description = "User id to own the scenario (default: ${DEFAULT-VALUE})")
  private int owner = 1;

  @Option(names = { "--url" },
      description = "AppLand website URL (default: ${DEFAULT-VALUE})")
  private String url = "https://appland-staging.herokuapp.com";

  @Override
  public Void call() {
    return null;
  }
}