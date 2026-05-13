package com.appland.appmap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
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
import java.util.List;
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
    Path agentJarPath = locateAgentJar();
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

  /**
   * Locate the agent jar on disk so its bundled runtime jar can be extracted and added to the
   * bootstrap class loader.
   *
   * <p>Prefer {@code Class.getResource} on this class because it works even when the agent has
   * been loaded by the bootstrap class loader (where {@code Class.getClassLoader()} returns
   * null). Fall back to parsing {@code -javaagent:} out of the JVM input arguments when
   * {@code getResource} resolves to a {@code file:} URL — which happens when this class is
   * also visible on a classpath directory, e.g. during integration tests where the agent jar
   * is attached via {@code -javaagent:} but the build also puts {@code build/classes/java/main}
   * on the test runtime classpath.
   *
   * @return the agent jar path, or {@code null} if no jar could be identified
   */
  private static Path locateAgentJar() {
    try {
      URL resourceURL = Agent.class.getResource(
          "/" + Agent.class.getName().replace('.', '/') + ".class");
      if (resourceURL != null && "jar".equals(resourceURL.getProtocol())) {
        String resourcePath = resourceURL.getPath();
        URL jarURL = new URL(resourcePath.substring(0, resourcePath.indexOf('!')));
        logger.debug("jarURL: {}", jarURL);
        return Paths.get(jarURL.toURI());
      }
    } catch (URISyntaxException | MalformedURLException e) {
      logger.error(e, "Failed getting path to agent jar");
      System.exit(1);
    }

    Path fromArgs = agentJarFromJvmArgs();
    if (fromArgs != null) {
      logger.debug("agent jar from -javaagent: {}", fromArgs);
    }
    return fromArgs;
  }

  /**
   * Parse {@code -javaagent:<path>[=options]} out of the JVM input arguments and return the path
   * if it points at an existing jar that declares {@code Premain-Class: com.appland.appmap.Agent}.
   *
   * @return the agent jar path or {@code null} if no matching {@code -javaagent} arg is present
   */
  private static Path agentJarFromJvmArgs() {
    final String prefix = "-javaagent:";
    List<String> jvmArgs;
    try {
      jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    } catch (SecurityException e) {
      logger.warn(e, "Unable to read JVM input arguments");
      return null;
    }
    for (String arg : jvmArgs) {
      if (!arg.startsWith(prefix)) {
        continue;
      }
      String spec = arg.substring(prefix.length());
      int eq = spec.indexOf('=');
      String pathPart = eq < 0 ? spec : spec.substring(0, eq);
      Path candidate = Paths.get(pathPart);
      if (!Files.isRegularFile(candidate)) {
        continue;
      }
      try (JarFile jf = new JarFile(candidate.toFile())) {
        if (jf.getManifest() == null) {
          continue;
        }
        String premain = jf.getManifest().getMainAttributes().getValue("Premain-Class");
        if (Agent.class.getName().equals(premain)) {
          return candidate.toAbsolutePath();
        }
      } catch (IOException e) {
        logger.debug(e, "Skipping unreadable -javaagent jar {}", candidate);
      }
    }
    return null;
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

    // It's critical to append the runtime JAR to the bootstrap class loader
    // search path, not the system class loader search path. This ensures that
    // AppMap's core runtime classes, such as HookFunctions, are available to
    // all application classes, including those loaded by different class loaders
    // (e.g., in web servers like Tomcat or other complex environments), which
    // fixes `NoClassDefFoundError` for `HookFunctions`.
    JarFile runtimeJar = new JarFile(runtimeJarPath.toFile());
    inst.appendToBootstrapClassLoaderSearch(runtimeJar);

    // HookFunctions can only be referenced after the runtime jar has been
    // appended to the boot class loader.
    HookFunctions.onMethodCall = MethodCall::onCall;
    HookFunctions.onMethodReturn = MethodReturn::onReturn;
  }
}
