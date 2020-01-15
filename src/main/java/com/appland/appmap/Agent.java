package com.appland.appmap;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.record.RuntimeRecorder;
import com.appland.appmap.transform.ClassFileTransformer;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * Agent is a JVM agent which instruments, records, and prints appmap files
 * for a program. To use the AppMap agent, start the progress with the JVM argument
 * <code>-javaagent:/path/to/appmap-java.jar</code>. The agent will read
 * the <code>appmap.yml</code> configuration file, which tells it which classes
 * to instrument. Classes will be instrumented automatically as they are loaded by the
 * JVM. As instrumented classes are used by the program, the activity is recorded by the agent.
 * In some cases, such as JUnit, AppMap files will be printed as the program executes.
 * When the agent exits, any un-printed data will be written to the file <code>appmap.json</code>.
 */
public class Agent {

  private final static String DEFAULT_CONFIG_FILE = "appmap.yml";

  /**
   * premain is the entry point for the AppMap Java agent.
   * @param agentArgs agent options
   * @param inst services needed to instrument Java programming language code
   * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html">Package java.lang.instrument</a>
   */
  public static void premain(String agentArgs, Instrumentation inst) {
    System.err.println("AppMap agent is loaded");

    String appmapPath = System.getProperty("appmap.config.file");
    if (appmapPath == null) {
      appmapPath = DEFAULT_CONFIG_FILE;
    }

    System.err.println("Reading appmap configuration from " + appmapPath);

    if (AppMapConfig.load(new File(appmapPath)) == null) {
      System.err.printf("AppMap: failed to load config %s\n", appmapPath);
      return;
    }

    inst.addTransformer(new ClassFileTransformer());

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();
          if (runtimeRecorder.isEmpty()) {
            return;
          }

          runtimeRecorder.flushToFile("appmap.json");
        }
    }, "Shutdown-thread"));
  }
}