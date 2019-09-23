package land.app.appmap.commands;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "inspect",
    description = "Inspect code and generate a classmap file")
public class Inspect implements Callable<Void> {
  @Option(names = { "-o", "--output" },
      description = "Name of the output file (default: ${DEFAULT-VALUE})")
  private File filename = new File("appmap.json");

  @Parameters(description = "Name of the executable archive to inspect")
  private File executable;

  @Override
  public Void call() {
    return null;
  }
}