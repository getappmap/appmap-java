package com.appland.appmap;

import java.lang.instrument.Instrumentation;

import java.io.File;

import com.appland.appmap.commands.Inspect;
import com.appland.appmap.commands.Record;
import com.appland.appmap.commands.Upload;
import com.appland.appmap.trace.Agent;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.trace.TraceListenerDebug;
import com.appland.appmap.trace.TraceListenerRecord;
import com.appland.appmap.trace.TraceClassTransformer;

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

  public static void premain(String agentArgs, Instrumentation inst) {
    AppMapConfig config = AppMapConfig.load(new File("appmap.yml"));
    if (config == null) {
      return;
    }

    TraceListenerRecord recording = new TraceListenerRecord();
    Agent agent = Agent.get()
        .config(config)
        .addListener(recording);

    if (System.getenv("APPMAP_DEBUG") != null) {
      agent.addListener(new TraceListenerDebug());
    }

    inst.addTransformer(new TraceClassTransformer());

    // agent.initialize();

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          System.out.println(recording.serialize());
        }
    }, "Shutdown-thread"));
  }
}