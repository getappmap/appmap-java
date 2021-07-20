package com.appland.appmap.cli;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@CommandLine.Command(name = "init", description = "Suggests AppMap configuration settings for a new project.")
public class Init implements Callable<Integer> {
  @CommandLine.ParentCommand
  private CLI parent;

  static class Configuration {
    @JSONField
    public String filename;

    @JSONField
    public String contents;
  }

  static class Result {
    @JSONField
    public Configuration configuration;
  }

  public Integer call() throws Exception {
    System.err.printf("Init AppMap project configuration in directory: %s\n", parent.directory);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("# This is the AppMap configuration file.");
    pw.println("# For full documentation of this file for Java programs, see:");
    pw.println("# https://appland.com/docs/reference/appmap-java.html#configuration");
    pw.format("name: %s\n", CLI.projectName(new File(parent.directory)));

    // For now, this only works in this type of standardize repo structure.
    File javaDir = new File("src/main/java");
    if (javaDir.isDirectory()) {
      // Collect package names in src/main/java
      Set<Path> packages = new HashSet<>();
      Files.walkFileTree(javaDir.toPath(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (file.getFileName().toString().endsWith(".java")) {
            Path packagePath = pathPackage(file.getParent());
            if (packagePath != null) {
              packages.add(packagePath);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });

      pw.println("# Your project contains the directory src/main/java.");
      pw.println("# AppMap has auto-detected the following Java packages in this directory:");
      pw.println("packages:");
      // Collect just the packages that don't have a matching ancestor in the package list.
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

    Configuration configuration = new Configuration();
    configuration.filename = "appmap.yml";
    configuration.contents = sw.toString();
    Result result = new Result();
    result.configuration = configuration;

    System.out.println(JSON.toJSONString(result, SerializerFeature.PrettyFormat));

    return 0;
  }

  private static Path pathPackage(Path dir) {
    if (dir.getNameCount() <= 3) {
      return null;
    }

    return dir.subpath(3, dir.getNameCount());
  }
}
