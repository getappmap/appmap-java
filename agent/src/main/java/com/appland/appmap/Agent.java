package com.appland.appmap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.appland.appmap.util.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.tinylog.TaggedLogger;
import org.tinylog.provider.ProviderRegistry;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.process.hooks.MethodCall;
import com.appland.appmap.process.hooks.MethodReturn;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recorder.Metadata;
import com.appland.appmap.record.Recording;
import com.appland.appmap.runtime.HookFunctions;
import com.appland.appmap.transform.ClassFileTransformer;
import com.appland.appmap.transform.annotations.HookFactory;
import com.appland.appmap.transform.instrumentation.BBTransformer;
import com.appland.appmap.util.GitUtil;

import javassist.CtClass;

public class Agent {

  public static final TaggedLogger logger = AppMapConfig.getLogger(null);

  /**
   * premain is the entry point for the AppMap Java agent.
   *
   * @param agentArgs agent options
   * @param inst services needed to instrument Java programming language code
   * @see <a href=
   *      "https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html">Package
   *      java.lang.instrument</a>
   */
  public static void premain(String agentArgs, Instrumentation inst) {

    long start = System.currentTimeMillis();
    try {
      AppMapConfig.initialize(FileSystems.getDefault());
    } catch (IOException e) {
      logger.warn(e, "Initialization failed");
      System.exit(1);
    }
    if (Properties.DisableLogFile == null) {
      // The user hasn't made a choice about disabling logging, let them know
      // they can.
      logger.info(
          "To disable the automatic creation of this log file, set the system property {} to 'true'",
          Properties.DISABLE_LOG_FILE_KEY);
    }
    String implementationVersion = Agent.class.getPackage().getImplementationVersion();
    Logger.printUserMessage("AppMap agent version %s starting\n", implementationVersion);
    logger.info("Agent version {}, current time mills: {}",
            implementationVersion, start);
    logger.info("config: {}", AppMapConfig.get());
    logger.debug("System properties: {}", System.getProperties());

    if (Agent.class.getClassLoader() == null) {
      logger.warn("AppMap agent is running on the bootstrap classpath. This is not a recommended configuration and should only be used for troubleshooting. Git integration will be disabled.");
    }

    addAgentJars(agentArgs, inst);


    if (!Properties.DisableGit) {
      try {
        GitUtil.findSourceRoots();
        logger.debug("done finding source roots, {}", () -> {
          long now = System.currentTimeMillis();
          return String.format("%d, %d", now - start, start);
        });
      } catch (IOException e) {
        logger.warn(e);
      }
    }

    if (Properties.SaveInstrumented) {
      CtClass.debugDump =
          Paths.get(System.getProperty("java.io.tmpdir"), "appmap", "ja").toString();
      logger.info("Saving instrumented files to {}", CtClass.debugDump);

    }

    // First, install a javassist-based transformer that will annotate app
    // methods that require instrumentation.
    ClassFileTransformer methodCallTransformer =
        new ClassFileTransformer("method call", HookFactory.APP_HOOKS_FACTORY);
    inst.addTransformer(methodCallTransformer);

    // Next, install a bytebuddy-based transformer that will instrument the
    // annotated methods.
    BBTransformer.installOn(inst);

    // Finally, install another javassist-based transformer that will instrument
    // non-app methods that have been hooked, i.e. those that have been
    // specified in com.appland.appmap.process.
    ClassFileTransformer systemHookTransformer =
        new ClassFileTransformer("system hook", HookFactory.AGENT_HOOKS_FACTORY);
    inst.addTransformer(systemHookTransformer);

    Runnable logShutdown = () -> {
      try {
        ClassFileTransformer.logStatistics();

        ProviderRegistry.getLoggingProvider().shutdown();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    };

    if (Properties.RecordingAuto) {
      startAutoRecording(logShutdown);
    }
    else {
      Runtime.getRuntime().addShutdownHook(new Thread(logShutdown));
    }
  }

  private static void startAutoRecording(Runnable logShutdown) {
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
  }

  private static void addAgentJars(String agentArgs, Instrumentation inst) {

    Path agentJarPath = null;
    try {
      Class<Agent> agentClass = Agent.class;
      // When the agent is loaded by the bootstrap class loader (e.g., via -Xbootclasspath/a:),
      // agentClass.getClassLoader() returns null, leading to a NullPointerException. To handle
      // this, we use Class.getResource() which correctly resolves resources even when the
      // class is loaded by the bootstrap class loader. The leading '/' in the resource name
      // is crucial for absolute path resolution when using Class.getResource().
      URL resourceURL = agentClass.getResource("/" + agentClass.getName().replace('.', '/') + ".class");

      // During testing of the agent itself, classes get loaded from a directory, and will have the
      // protocol "file". The rest of the time (i.e. when it's actually deployed), they'll always
      // come from a jar file. We must also check that resourceURL is not null before using it,
      // as getResource() can return null if the resource is not found.
      if (resourceURL != null && resourceURL.getProtocol().equals("jar")) {
        String resourcePath = resourceURL.getPath();
        URL jarURL = new URL(resourcePath.substring(0, resourcePath.indexOf('!')));
        logger.debug("jarURL: {}", jarURL);
        agentJarPath = Paths.get(jarURL.toURI());
      }
    } catch (URISyntaxException | MalformedURLException e) {
      // Doesn't seem like these should ever happen....
      logger.error(e, "Failed getting path to agent jar");
      System.exit(1);
    }
    if (agentJarPath != null) {
      try {
        JarFile agentJar = new JarFile(agentJarPath.toFile());
        inst.appendToSystemClassLoaderSearch(agentJar);

        setupRuntime(agentJarPath, agentJar, inst);
      } catch (IOException | SecurityException | IllegalArgumentException e) {
        logger.error(e, "Failed loading agent jars");
        System.exit(1);
      }
    }
  }

  private static void setupRuntime(Path agentJarPath, JarFile agentJar, Instrumentation inst)
      throws IOException, FileNotFoundException {
    Path runtimeJarPath = null;
    for (Enumeration<JarEntry> entries = agentJar.entries(); entries.hasMoreElements();) {
      JarEntry entry = entries.nextElement();
      String entryName = entry.getName();
      if (entryName.startsWith("runtime-")) {
        Path installDir = agentJarPath.getParent();
        runtimeJarPath = installDir.resolve(FilenameUtils.getBaseName(entryName) + ".jar");
        if (!Files.exists(runtimeJarPath)) {
          IOUtils.copy(agentJar.getInputStream(entry),
              new FileOutputStream(runtimeJarPath.toFile()));
        }
        break;
      }
    }

    if (runtimeJarPath == null) {
      logger.error("Couldn't find runtime jar in {}", runtimeJarPath);
      System.exit(1);
    }

    // Adding the runtime jar to the boot class loader means the classes it
    // contains will be available everywhere. This avoids issues caused by any
    // filtering the app's class loader might be doing (e.g. the Scala runtime
    // when running a Play app).
    JarFile runtimeJar = new JarFile(runtimeJarPath.toFile());
    inst.appendToSystemClassLoaderSearch(runtimeJar);
    // inst.appendToBootstrapClassLoaderSearch(runtimeJar);

    // HookFunctions can only be referenced after the runtime jar has been
    // appended to the boot class loader.
    HookFunctions.onMethodCall = MethodCall::onCall;
    HookFunctions.onMethodReturn = MethodReturn::onReturn;
  }
}
