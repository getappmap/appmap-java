package com.appland.appmap;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recorder.Metadata;
import com.appland.appmap.record.Recording;
import com.appland.appmap.transform.ClassFileTransformer;
import com.appland.appmap.util.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;

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
  /**
   * premain is the entry point for the AppMap Java agent.
   * @param agentArgs agent options
   * @param inst services needed to instrument Java programming language code
   * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html">Package java.lang.instrument</a>
   */
  public static void premain(String agentArgs, Instrumentation inst) {

    final File dir = Properties.getOutputDirectory();

    Logger.println("Agent version " + Agent.class.getPackage().getImplementationVersion());
    Logger.println("System properties: " + System.getProperties().toString());
    if (Properties.Debug) {
      Logger.whereAmI();
    }
    
    inst.addTransformer(new ClassFileTransformer());

    if (AppMapConfig.load(new File(Properties.ConfigFile)) == null) {
      Logger.printf("failed to load config %s\n", Properties.ConfigFile);
      System.exit(1);
    }

    if (Properties.RecordingAuto) {
      String appmapName = Properties.RecordingName;
      final Date date = new Date();
      final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
      final String timestamp = dateFormat.format(date);
      final Metadata metadata = new Metadata();
      final Recorder recorder = Recorder.getInstance();

      if (appmapName == null || appmapName.trim().isEmpty()) {
        appmapName = timestamp;
      }

      metadata.recorderName = "remote_recording";
      metadata.scenarioName = appmapName;

      recorder.start(metadata);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        String fileName = Properties.RecordingFile;

        if (fileName == null || fileName.trim().isEmpty()) {
          fileName = String.format("%s.appmap.json", timestamp);
        }

        Recording recording = recorder.stop();
        recording.moveTo(String.join(File.pathSeparator, new String[]{ dir.getPath(), fileName }));
      }));
    }
  }
}
