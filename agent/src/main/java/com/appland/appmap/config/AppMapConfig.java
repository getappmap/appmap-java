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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import org.tinylog.configuration.Configuration;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.Agent;
import com.appland.appmap.cli.CLI;
import com.appland.appmap.util.FullyQualifiedName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javassist.CtBehavior;

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

  /**
   * Search for the config file for the current application.
   *
   * First, check to see if the specified config file exists. If it does, return it. If it does not,
   * and mustExist is true, throw a FileNotFoundException.
   *
   * If it does not, scan up the directory tree. If a file named appmap.yml is found, return it.
   * Otherwise, throw a FileNotFoundException.
   *
   * @param configFile the starting config file path
   * @param mustExist if true, throw a FileNotFoundExcpetion if configFile does not exist
   * @return the absolute path to the discovered config file
   * @throws FileNotFoundException if mustExist is true and configFile does not exist, or configFile
   *         does not exist, and no appmap.yml is found in any of the parent directories
   */
  private static Path findConfig(Path configFile, boolean mustExist) throws FileNotFoundException {
    if (Files.exists(configFile)) {
      return configFile.toAbsolutePath();
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

      return configFile.toAbsolutePath();
    } catch (IOException e) {
      logger.error(e, "Failed to create default config");
    }

    throw new FileNotFoundException(configFile.toString());
  }

  /**
   * Populate the configuration from a file.
   *
   * @param configFile The file to be loaded
   * @param mustExist When true, the config must already exist; when false, it will be created if it
   *        doesn't exist.
   *
   * @return The AppMapConfig singleton
   */
  static AppMapConfig load(Path configFile, boolean mustExist) {
    InputStream inputStream = null;

    try {
      configFile = AppMapConfig.findConfig(configFile, mustExist);
      logger.debug("using config file -> {}", configFile);
      inputStream = Files.newInputStream(configFile);
    } catch (IOException e) {
      Path expectedConfig = configFile;
      logger.error("error: file not found -> {}",
          expectedConfig);
      return null;
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      singleton = mapper.readValue(inputStream, AppMapConfig.class);
      if (singleton.packages == null) {
        logger.error("AppMap: missing value for the 'packages' entry in appmap.yml");
        return null;
      }
    } catch (IOException e) {
      logger.error("AppMap: encountered syntax error in appmap.yml {}", e.getMessage());
      return null;
    }
    singleton.configFile = configFile;
    logger.debug("config: {}", singleton);

    int count = singleton.packages.length;
    count = Arrays.stream(singleton.packages).map(p -> p.exclude).reduce(count,
        (acc, e) -> acc += e.length, Integer::sum);

    int pattern_threshold = Properties.PatternThreshold;
    if (count > pattern_threshold) {
      logger.warn("{} patterns found in config, startup performance may be impacted", count);
    }

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
   * Check if a method is explicitly excluded by the configuration.
   *
   * @param behavior the method to be checked
   * @return {@code true} if the method is explicitly excluded, {@code false} otherwise
   */
  public Boolean excludes(CtBehavior behavior) {
    if (this.packages == null) {
      return false;
    }

    for (AppMapPackage pkg : this.packages) {
      if (pkg.excludes(behavior)) {
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
    pw.println("# https://appmap.io/docs/reference/appmap-java.html#configuration");
    pw.format("name: %s\n", CLI.projectName(new File(directory)));

    // Set to collect packages from all relevant src/main/java directories
    Set<Path> packages = new HashSet<>();
    AtomicBoolean srcMainJavaDirExists = new AtomicBoolean(false);

    // Traverse the root directory to find all src/main/java directories
    Files.walkFileTree(Paths.get(directory), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (dir.endsWith("src/main/java")) {
          srcMainJavaDirExists.set(true);
          collectPackages(dir, packages);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException io)
      {
        return FileVisitResult.SKIP_SUBTREE;
      }
    });
    
    if (srcMainJavaDirExists.get()) {
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

  private static void collectPackages(Path javaDir, Set<Path> packages) {
    int pkgStart = javaDir.getNameCount();
    try {
        Files.walkFileTree(javaDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(".java")) {
                    int pkgEnd = file.getParent().getNameCount();
                    if (pkgStart == pkgEnd) {
                        // We're in the unnamed package, ignore
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
    } catch (IOException e) {
        e.printStackTrace();
    }
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

    String outputDirectory = Properties.resolveProperty(APPMAP_OUTPUT_DIRECTORY_KEY, (String)null);
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
    Properties.OutputDirectory = singleton.configFile.getParent().resolve(singleton.appmapDir);

  }

  private static Path findDefaultOutputDirectory(FileSystem fs) {
    return fs.getPath("tmp/appmap");
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("name: ").append(name).append("\n");
    if (configFile != null) {
      sb.append("configFile: ").append(configFile).append("\n");
    }
    sb.append("packages: ");
    if (packages == null || packages.length == 0) {
      sb.append("[]");
    } else {
      for (AppMapPackage pkg : packages) {
        sb.append("\n  - path: ").append(pkg.path);
        if (pkg.shallow) {
          sb.append("\n    shallow: true");
        }
        if (pkg.exclude != null && pkg.exclude.length > 0) {
          sb.append("\n    exclude: ").append(Arrays.toString(pkg.exclude));
        }
      }
    }
    return sb.toString();
  }
}
