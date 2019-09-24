package com.appland.appmap;

import com.appland.appmap.commands.Inspect;
import com.appland.appmap.commands.Record;
import com.appland.appmap.commands.Upload;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "appmap",
    mixinStandardHelpOptions = true,
    subcommands = {
      Inspect.class,
      Record.class,
      Upload.class,
})
public class App implements Runnable {

  @Override public void run() { }

  /**
   * This method is the entry point of the CLI.
   */
  public static void main(String[] args) {
    CommandLine cmd = new CommandLine(new App());
    if (args.length == 0) {
      cmd.usage(System.out);
      return;
    }

    Integer exitCode = cmd.execute(args);
    System.exit(exitCode);
  }
}