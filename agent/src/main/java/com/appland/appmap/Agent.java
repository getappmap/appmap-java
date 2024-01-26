package com.appland.appmap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    logger.info("Agent version {}, current time mills: {}",
        Agent.class.getPackage().getImplementationVersion(), start);
    logger.info("config: {}", AppMapConfig.get());
    logger.info("System properties: {}", System.getProperties());
    logger.debug(new Exception(), "whereAmI");

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
    ProtectionDomain protectionDomain = Agent.class.getProtectionDomain();
    CodeSource codeSource;
    URL jarURL;
    if (((codeSource = protectionDomain.getCodeSource()) == null)
        || ((jarURL = codeSource.getLocation()) == null)) {
      // Nothing we can do if we can't locate the agent jar
      return;
    }

    Path agentJarPath = null;
    try {
      agentJarPath = Paths.get(jarURL.toURI());
    } catch (URISyntaxException e) {
      // Doesn't seem like this should ever happen....
      System.err.println("Failed getting path to agent jar");
      e.printStackTrace();
      System.exit(1);
    }
    // During testing of the agent itself, classes get loaded from a directory.
    // The rest of the time (i.e. when it's actually deployed), they'll always
    // come from a jar file.
    JarFile agentJar = null;
    if (!Files.isDirectory(agentJarPath)) {
      try {
        agentJar = new JarFile(agentJarPath.toFile());
        inst.appendToSystemClassLoaderSearch(agentJar);

        setupRuntime(agentJarPath, agentJar, inst);
      } catch (IOException | SecurityException | IllegalArgumentException e) {
        System.err.println("Failed loading agent jars");
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  private static void setupRuntime(Path agentJarPath, JarFile agentJar, Instrumentation inst)
      throws IOException, FileNotFoundException {
    Path runtimeJar = null;
    for (Enumeration<JarEntry> entries = agentJar.entries(); entries.hasMoreElements();) {
      JarEntry entry = entries.nextElement();
      String entryName = entry.getName();
      if (entryName.startsWith("runtime-")) {
        Path installDir = agentJarPath.getParent();
        runtimeJar = installDir.resolve(FilenameUtils.getBaseName(entryName) + ".jar");
        if (!Files.exists(runtimeJar)) {
          IOUtils.copy(agentJar.getInputStream(entry),
              new FileOutputStream(runtimeJar.toFile()));
        }
        break;
      }
    }

    if (runtimeJar == null) {
      System.err.println("Couldn't find runtime jar in " + agentJarPath);
      System.exit(1);
    }

    // Adding the runtime jar to the boot class loader means the classes it
    // contains will be available everywhere. This avoids issues caused by any
    // filtering the app's class loader might be doing (e.g. the Scala runtime
    // when running a Play app).
    inst.appendToBootstrapClassLoaderSearch(new JarFile(runtimeJar.toFile()));

    // HookFunctions can only be referenced after the runtime jar has been
    // appended to the boot class loader.
    HookFunctions.onMethodCall = MethodCall::onCall;
    HookFunctions.onMethodReturn = MethodReturn::onReturn;
  }
}
