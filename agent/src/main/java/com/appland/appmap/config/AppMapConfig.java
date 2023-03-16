package com.appland.appmap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.appland.appmap.cli.CLI;
import com.appland.appmap.util.FullyQualifiedName;
import com.appland.appmap.util.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppMapConfig {
  public File configFile;  // the configFile used
  public String name;
  public AppMapPackage[] packages = new AppMapPackage[0];
  private static AppMapConfig singleton = new AppMapConfig();

  static File findConfig(File configFile, boolean mustExist) throws FileNotFoundException {
    if (configFile.exists()) {
      return configFile;
    }

    if (mustExist) {
      throw new FileNotFoundException(configFile.toString());
    }

    Path projectDirectory = configFile.toPath().toAbsolutePath().getParent();
    Path parent = projectDirectory;
    while (parent != null) {
      Path c = parent.resolve("appmap.yml");
      if (Files.exists(c)) {
        return c.toFile();
      }
      parent = parent.getParent();
    }

    try (
        FileWriter fw = new FileWriter(configFile)) {
      Files.createDirectories(projectDirectory);
      fw.write(getDefault(projectDirectory.toString()));

      return configFile;
    } catch (IOException e) {
      Logger.error("Failed to create default config\n");
      Logger.error(e);
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
  public static AppMapConfig load(File configFile, boolean mustExist) {
    InputStream inputStream = null;

    try {
      configFile = AppMapConfig.findConfig(configFile, mustExist);
      Logger.println(String.format("using config file -> %s",
                                   configFile.getAbsolutePath()));
      inputStream = new FileInputStream(configFile);
    } catch (FileNotFoundException e) {
      String expectedConfig = configFile.getAbsolutePath();
      Logger.println(String.format("error: file not found -> %s",
                                   expectedConfig));
      Logger.error(String.format("error: file not found -> %s",
                                 expectedConfig));
      return null;
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      singleton = mapper.readValue(inputStream, AppMapConfig.class);
    } catch (IOException e) {
      Logger.error("AppMap: encountered syntax error in appmap.yml " + e.getMessage());
      System.exit(1);
    }
    singleton.configFile = configFile;

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
    File javaDir = Paths.get(directory).resolve("src/main/java").toFile();
    if (javaDir.isDirectory()) {
      int pkgStart = javaDir.toPath().getNameCount();
      // Collect package names in src/main/java
      Set<Path> packages = new HashSet<>();
      Files.walkFileTree(javaDir.toPath(), new SimpleFileVisitor<Path>() {
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
          + "# auto-detected Java packages. By default, only classes in these\n"
          + "# packages will be recorded. To record classes in subpackages, remove\n"
          + "# the line(s)\n"
          + "#   shallow: true\n"
          + "\n"
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
          pw.format("  shallow: true\n");
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
}
