package com.appland.appmap.commands;

import com.appland.appmap.debugger.Trace;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "inspect",
    description = "Inspect code and generate a classmap file")
public class Inspect implements Callable<Void> {
  @Option(names = { "-o", "--output" },
      description = "Name of the output file (default: ${DEFAULT-VALUE})")
  private File filename = new File("appmap.json");

  @Option(names = { "-c", "--class-path" },
      description = String.join(" ",
        "Specifies a list of directories, JAR archives, and ZIP archives to",
        "search for class files. Class path entries are separated by colons (:)."))
  private String[] classPath;

  @Option(names = { "-j", "--jar" },
      description = "Name of the jar file to be invoked")
  private File jarPath;

  @Option(names = { "-m", "--main" },
      description = "Name of the main class")
  private String mainClass;

  @Override
  public Void call() {
    Trace trace = new Trace();

    if (jarPath != null) {
      if (classPath != null && classPath.length > 0) {
        System.err.println("warn: both jar and class path options provided.");
        System.err.println("      only jar will be used.");
      }
      trace.execute(jarPath);
      return null;
    }

    if (mainClass == null || mainClass.isBlank()) {
      System.err.println("error: a jar path or main class must be provided");
      return null;
    }

    if (classPath != null && classPath.length > 0) {
      String joinedClassPath = String.join(":", classPath);
      trace.execute(mainClass, joinedClassPath);
      return null;
    }

    trace.execute(mainClass);
    return null;
  }
}