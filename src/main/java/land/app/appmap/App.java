package land.app.appmap;

import land.app.appmap.commands.*;

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