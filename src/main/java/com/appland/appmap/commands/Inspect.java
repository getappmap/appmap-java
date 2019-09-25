package com.appland.appmap.commands;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;

import com.appland.appmap.debugger.Trace;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "inspect",
    description = "Inspect code and generate a classmap file")
public class Inspect implements Callable<Void> {
  @Option(names = { "-o", "--output" },
      description = "Name of the output file (default: ${DEFAULT-VALUE})")
  private File filename = new File("appmap.yml");

  @Option(names = { "-c", "--class-path" },
      description = "Specifies a list of directories, JAR archives, and ZIP archives to search for class files. Class path entries are separated by colons (:).")
  private String[] classPath;

  @Option(names = { "-j", "--jar" },
      description = "Name of the jar file to be invoked")
  private File jarPath;

  @Option(names = { "-m", "--main" },
      description = "Name of the main class")
  private String mainClass;

  @Option(names = { "-a", "--args" }, description = "launch arguments")
  private String[] launchArgs = new String[]{};

  @Override
  public Void call() {
    AppMapConfig config = AppMapConfig.load(filename);
    if (config == null) {
      return null;
    }

    Trace trace = new Trace();

    ArrayList<String> classPaths = new ArrayList<String>();
    for (AppMapPackage p : config.packages) {
      trace.includeClassPath(p.path);

      if (p.exclude == null) {
        continue;
      }

      for (String exclusion : p.exclude) {
        trace.excludeClassPath(exclusion);
      }
    }

    if (jarPath != null) {
      if (classPath != null && classPath.length > 0) {
        System.err.println("warn: both jar and class path options provided.");
        System.err.println("      only jar will be used.");
      }
      trace.execute(jarPath, launchArgs);
      return null;
    }

    if (mainClass == null || mainClass.isBlank()) {
      System.err.println("error: a jar path or main class must be provided");
      return null;
    }

    mainClass = String.format("%s", mainClass, String.join(" ", launchArgs));

    if (classPath != null && classPath.length > 0) {
      // if -c is provided many times, join them
      String joinedClassPath = String.join(":", classPath);
      trace.execute(mainClass, joinedClassPath);
      return null;
    }

    trace.execute(mainClass);

    System.out.println(trace.serialize());

    return null;
  }
}