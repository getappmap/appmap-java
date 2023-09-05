package com.appland.appmap.config;

import static com.appland.appmap.config.Properties.APPMAP_OUTPUT_DIRECTORY_KEY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import org.tinylog.configuration.Configuration;

import com.appland.appmap.Agent;
import com.appland.appmap.cli.CLI;
import com.appland.appmap.util.FullyQualifiedName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppMapConfig {
  private static final TaggedLogger logger = configureLogging();

  public Path configFile; // the configFile used
  public String name;
  public AppMapPackage[] packages = new AppMapPackage[0];

  private String appmapDir;

  @JsonProperty("appmap_dir")
  String getAppmapDir() {
    return appmapDir;
  }

  private static AppMapConfig singleton = new AppMapConfig();

  public static org.tinylog.TaggedLogger getLogger(String tag) {
    return org.tinylog.Logger.tag(tag);
  }

  private static Path findConfig(Path configFile, boolean mustExist) throws FileNotFoundException {
    if (Files.exists(configFile)) {
      return configFile;
    }

    if (mustExist) {
      throw new FileNotFoundException(configFile.toString());
    }

    Path projectDirectory = configFile.toAbsolutePath().getParent();
    Path parent = projectDirectory;
    while (parent != null) {
      Path c = parent.resolve("appmap.yml");
      if (Files.exists(c)) {
        return c;
      }
      parent = parent.getParent();
    }

    try {
      Files.createDirectories(projectDirectory);
      Files.write(configFile, getDefault(projectDirectory.toString()).getBytes());

      return configFile;
    } catch (IOException e) {
      logger.error(e, "Failed to create default config");
    }

    throw new FileNotFoundException(configFile.toString());
  }

  /**
   * Populate the configuration from a file.
   * 
   * @param configFile The file to be loaded
   * @param mustExist  When true, the config must already exist; when false, it
   *                   will be created if it doesn't exist.
   *
   * @return The AppMapConfig singleton
   */
  static AppMapConfig load(Path configFile, boolean mustExist) {
    InputStream inputStream = null;

    try {
      configFile = AppMapConfig.findConfig(configFile, mustExist);
      logger.debug("using config file -> {}", configFile.toAbsolutePath());
      inputStream = Files.newInputStream(configFile);
    } catch (IOException e) {
      Path expectedConfig = configFile.toAbsolutePath();
      logger.error("error: file not found -> {}",
          expectedConfig);
      return null;
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      singleton = mapper.readValue(inputStream, AppMapConfig.class);
    } catch (IOException e) {
      logger.error("AppMap: encountered syntax error in appmap.yml {}", e.getMessage());
      System.exit(1);
    }
    singleton.configFile = configFile;
    logger.debug("config: {}", singleton);
    return singleton;
  }

  /**
   * Get the AppMapConfig singleton.
   * @return The singleton instance
   */
  public static AppMapConfig get() {
    return singleton;
  }

  /**
   * Check if a class/method is included in the configuration.
   * @param canonicalName the canonical name of the class/method to be checked
   * @return {@code true} if the class/method is included in the configuration. {@code false} if it
   *         is not included or otherwise explicitly excluded.
   */
  public AppMapPackage.LabelConfig includes(FullyQualifiedName canonicalName) {
    if (this.packages == null) {
      return null;
    }

    for (AppMapPackage pkg : this.packages) {
      final AppMapPackage.LabelConfig ls = pkg.find(canonicalName);
      if (ls != null) {
        return ls;
      }
    }

    return null;
  }

  /**
   * Check if a class/method is explicitly excluded in the configuration.
   * @param canonicalName the canonical name of the class/method to be checked
   * @return {@code true} if the class/method is explicitly excluded in the configuration. Otherwise, {@code false}.
   */
  public Boolean excludes(FullyQualifiedName canonicalName) {
    if (this.packages == null) {
      return false;
    }

    for (AppMapPackage pkg : this.packages) {
      if (pkg.excludes(canonicalName)) {
        return true;
      }
    }

    return false;
  }

  public boolean isShallow(FullyQualifiedName canonicalName) {
    if (canonicalName == null) {
      return false;
    }
    for (AppMapPackage pkg : this.packages) {
      if (pkg.find(canonicalName) != null) {
        return pkg.shallow;
      }
    }

    return false;
  }

  public static String getDefault(String directory) throws IOException {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("# This is the AppMap configuration file.");
    pw.println("# For full documentation of this file for Java programs, see:");
    pw.println("# https://appland.com/docs/reference/appmap-java.html#configuration");
    pw.format("name: %s\n", CLI.projectName(new File(directory)));

    // For now, this only works in this type of standardize repo structure.
    Path javaDir = Paths.get(directory).resolve("src/main/java");
    if (Files.isDirectory(javaDir)) {
      int pkgStart = javaDir.getNameCount();
      // Collect package names in src/main/java
      Set<Path> packages = new HashSet<>();
      Files.walkFileTree(javaDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (file.getFileName().toString().endsWith(".java")) {
            int pkgEnd = file.getParent().getNameCount();
            if (pkgStart == pkgEnd) {
              // We're in the the unnamed package, ignore
              return FileVisitResult.CONTINUE;
            }

            Path packagePath = file.getParent().subpath(pkgStart, pkgEnd);
            if (packagePath.getNameCount() > 0) {
              packages.add(packagePath);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });

      pw.print("\n"
          + "# Your project contains the directory src/main/java. AppMap has\n"
          + "# auto-detected the following Java packages in this directory:\n"
          + "packages:\n");

      // Collect just the packages that don't have a matching ancestor in the package
      // list.
      List<Path> topLevelPackages = packages.stream().sorted().collect(ArrayList::new, (memo, packagePath) -> {
        for (int i = 1; i < packagePath.getNameCount(); i++) {
          Path ancestorPath = packagePath.subpath(0, i);
          if (memo.contains(ancestorPath)) {
            return;
          }
        }
        memo.add(packagePath);
      }, ArrayList::addAll);
      topLevelPackages.forEach(new Consumer<Path>() {
        @Override
        public void accept(Path packagePath) {
          List<String> tokens = new ArrayList<>();
          for (int i = 0; i < packagePath.getNameCount(); i++) {
            tokens.add(packagePath.getName(i).toString());
          }
          String path = String.join(".", tokens);
          pw.format("- path: %s\n", path);
        }
      });
    } else {
      pw.println("packages: []");
      pw.println("# appmap-java init looks for source packages in src/main/java.");
      pw.println("# This folder was not found in your project, so no packages were auto-detected.");
      pw.println("# You can add your source packages by replacing the line above with lines like this:");
      pw.println("# packages:");
      pw.println("# - path: com.mycorp.pkg");
      pw.println("# - path: org.otherstuff.pkg");
    }
    return sw.toString();
  }

  public static TaggedLogger configureLogging() {
    // tinylog freezes its configuration after the first call to any of its
    // methods other than those in Configuration. So, get everything ready
    // before returning the logger for this class;
    if (Properties.Debug) {
      Configuration.set("level", "debug");
    }

    if (Properties.DebugFile != null) {
      Configuration.set("writer", "file");
      Configuration.set("writer.file", Properties.DebugFile);
    }

    return Logger.tag(null);
  }

  public static void initialize(FileSystem fs) throws IOException {
    // If the user explicitly specified a config file, but the file doesn't
    // exist, raise an error. They've almost certainly made a mistake.
    boolean configSpecified = Properties.ConfigFile != null;
    String configFile = !configSpecified ? "appmap.yml" : Properties.ConfigFile;
    if (load(fs.getPath(configFile), configSpecified) == null) {
      Agent.logger.error("failed to load config {}", Properties.ConfigFile);
      System.exit(1);
    }

    String outputDirectory = System.getProperty(APPMAP_OUTPUT_DIRECTORY_KEY);
    if (outputDirectory  == null) {
      if (singleton.appmapDir == null) {
        singleton.appmapDir = findDefaultOutputDirectory(fs).toString();
      }
    } else {
      if (singleton.appmapDir != null) {
        if (!outputDirectory.equals(singleton.appmapDir)) {
          Agent.logger.warn("{} specified, and appmap.yml contains appmap_dir. Using {} as output directory.",
              APPMAP_OUTPUT_DIRECTORY_KEY, outputDirectory);
          singleton.appmapDir = outputDirectory;
        }
      } else {
        singleton.appmapDir = outputDirectory;
      }
    }
    Properties.OutputDirectory = fs.getPath(singleton.appmapDir);
  }

  private static Path findDefaultOutputDirectory(FileSystem fs) {
    long buildGradleLastModified = 0;
    long pomXmlLastModified = 0;
    try {
      buildGradleLastModified = Files.getLastModifiedTime(fs.getPath("build.gradle")).toMillis();
    } catch (NoSuchFileException e) {
      // Can't use logger yet, and this may happen regularly, so just swallow
      // it.
    } catch (IOException e) {
      // This shouldn't happen, though
      e.printStackTrace();
    }
    try {
      pomXmlLastModified = Files.getLastModifiedTime(fs.getPath("pom.xml")).toMillis();
    } catch (NoSuchFileException e) {
      // noop, as above
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Neither exists, use tmp
    if (buildGradleLastModified == 0 && pomXmlLastModified == 0) {
      return fs.getPath("tmp/appmap");
    }

    // Both exist, use newer
    String gradleDir = "build/tmp/appmap";
    String mavenDir = "target/tmp/appmap";
    if (buildGradleLastModified != 0 && pomXmlLastModified != 0) {
      if (buildGradleLastModified > pomXmlLastModified) {
        return fs.getPath(gradleDir);
      } else {
        return fs.getPath(mavenDir);
      }
    }

    // Might be Gradle
    if (buildGradleLastModified > 0) {
      return fs.getPath(gradleDir);
    }

    // Must be Maven
    return fs.getPath(mavenDir);
  }
}
