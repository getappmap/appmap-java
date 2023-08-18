package com.appland.appmap;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.tinylog.TaggedLogger;
import org.tinylog.provider.ProviderRegistry;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recorder.Metadata;
import com.appland.appmap.record.Recording;
import com.appland.appmap.transform.ClassFileTransformer;

import javassist.ClassPool;

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
  public static final TaggedLogger logger = AppMapConfig.getLogger(null);

  /**
   * premain is the entry point for the AppMap Java agent.
   * @param agentArgs agent options
   * @param inst services needed to instrument Java programming language code
   * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html">Package java.lang.instrument</a>
   */
  public static void premain(String agentArgs, Instrumentation inst) {
    logger.debug("Agent version {}", Agent.class.getPackage().getImplementationVersion());
    logger.debug("System properties: {}", System.getProperties());
    logger.debug(new Exception(), "whereAmI");

    logger.trace("Agent.class class loader: {}", Agent.class.getClassLoader());
    logger.trace("ClassPool.getDefault(): {}", ClassPool.getDefault());

    inst.addTransformer(new ClassFileTransformer());

    try {
      AppMapConfig.initialize(FileSystems.getDefault());
    } catch (IOException e) {
      logger.warn(e, "Initialization failed");
      System.exit(1);
    }

    Runnable logShutdown = () -> {
      try {
        ProviderRegistry.

            getLoggingProvider().shutdown();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    };

    if (Properties.RecordingAuto) {
      String appmapName = Properties.RecordingName;
      final Date date = new Date();
      final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
      final String timestamp = dateFormat.format(date);
      final Metadata metadata = new Metadata("java", "process");
      final Recorder recorder = Recorder.getInstance();

      if (appmapName == null || appmapName.trim().isEmpty()) {
        appmapName = timestamp;
      }

      metadata.scenarioName = appmapName;

      recorder.start(metadata);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        String fileName = Properties.RecordingFile;

        if (fileName == null || fileName.trim().isEmpty()) {
          fileName = String.format("%s.appmap.json", timestamp);
        }

        Recording recording = recorder.stop();
        recording.moveTo(fileName);

        logShutdown.run();
      }));
    } else {
      Runtime.getRuntime().addShutdownHook(new Thread(logShutdown));
    }
  }



}
