package com.appland.appmap;

import java.lang.instrument.Instrumentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import com.appland.appmap.commands.Inspect;
import com.appland.appmap.commands.Record;
import com.appland.appmap.commands.Upload;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.record.RuntimeRecorder;
import com.appland.appmap.transform.AppMapClassTransformer;
import com.appland.appmap.transform.SqlClassTransformer;
import com.appland.appmap.transform.HttpClassTransformer;
import com.appland.appmap.transform.TraceClassTransformer;

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

    TraceClassTransformer   traceTransformer = new TraceClassTransformer(config);
    SqlClassTransformer     sqlTransformer   = new SqlClassTransformer();
    HttpClassTransformer    httpTransformer  = new HttpClassTransformer();
    AppMapClassTransformer  transformer      = new AppMapClassTransformer();
    transformer.addSubTransform(sqlTransformer)
               .addSubTransform(httpTransformer)
               .addSubTransform(traceTransformer);

    // TraceListenerRecord recording = new TraceListenerRecord();
    // Agent agent = Agent.get()
    //     .config(config)
    //     .addListener(recording);

    // if (System.getenv("APPMAP_DEBUG") != null) {
    //   agent.addListener(new TraceListenerDebug());
    // }

    inst.addTransformer(transformer);

    // agent.initialize();

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();

          try {
            System.err.print("writing data to appmap.json... ");
            PrintWriter out = new PrintWriter("appmap.json");
            out.print(runtimeRecorder.dumpJson());
            out.close();

            System.err.print("done.\n");
          } catch (FileNotFoundException e) {
            System.err.printf("failed: %s\n", e.getMessage());
          } catch (Exception e) {
            System.err.printf("failed: %s\n", e.getMessage());
          }
        }
    }, "Shutdown-thread"));
  }
}