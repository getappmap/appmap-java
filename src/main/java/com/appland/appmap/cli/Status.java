package com.appland.appmap.cli;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.SystemUtils;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@CommandLine.Command(name = "status", description = "Prints AppMap status of the Java project in a specified directory.")
public class Status implements Callable<Integer> {
  @CommandLine.ParentCommand
  private CLI parent;

  static class Command {
    public String program;
    public List<String> args = new ArrayList<>();
    public Map<String, String> environment = new HashMap<>();
  }

  static class TestCommand {
    public String framework;
    public Command command;

    TestCommand(String framework) {
      this.framework = framework;
    }
  }

  static class ConfigStatus {
    public String app;
    public boolean present;
    public boolean valid;
  }

  static class AgentStatus {
    public String version = Status.class.getPackage().getImplementationVersion();
  }

  static class ProjectStatus {
    public String language = "java";
  }

  static class FrameworkStatus {
    public String name;
    public boolean present;
    public boolean valid;
  }

  static class Properties {
    public ConfigStatus config = new ConfigStatus();
    public AgentStatus agent = new AgentStatus();
    public ProjectStatus project = new ProjectStatus();
    public List<FrameworkStatus> frameworks = new ArrayList<>();

    static boolean isConfigValid(Path path) {
      // Start with simple YAML validation
      Yaml yaml = new Yaml();
      InputStream inputStream = null;
      try {
        inputStream = Files.newInputStream(path);
      } catch (IOException e) {
        // System.err.println(e);
        return false;
      }
      try {
        yaml.load(inputStream);
      } catch (RuntimeException e) {
        // TODO: Good validation messages are available here. An example:
        /*
        while scanning a simple key
         in 'reader', line 7, column 1:
            xx
            ^
        could not find expected ':'
         in 'reader', line 7, column 3:
            xx
              ^
        */
        // System.err.println(e);
        return false;
      }

      return true;
    }

    static boolean isGradlePresent(Path projectPath) {
      return Arrays.stream(new String[]{"build.gradle", "build.gradle.kts"})
          .map(new Function<String, Path>() {
            public Path apply(String pathName) {
              return projectPath.resolve(pathName);
            }
          })
          .filter((Path path) -> Files.exists(path))
          .anyMatch((path) -> {
            try {
              return Files.readAllLines(path).stream().anyMatch((line) -> line.contains("com.appland.appmap"));
            } catch (IOException e) {
              return false;
            }
          });
    }

    static boolean isGradleValid(Path projectPath) {
      // Our doc says that the user can run: gradle appmap test
      // Try and run gradle --help appmap
      return Arrays.stream(new String[]{"gradlew", "gradlew.bat"})
          .map(new Function<String, Path>() {
            public Path apply(String pathName) {
              return projectPath.resolve(pathName);
            }
          })
          .filter((Path path) -> Files.exists(path))
          .anyMatch((commandPath) -> {
            try {
              String command = String.format("%s --help appmap", commandPath);
              // System.err.printf("Attempting: %s\n", command);
              Process process = Runtime.getRuntime().exec(command);
              process.waitFor(60, TimeUnit.SECONDS);
              return process.exitValue() == 0;
            } catch (IOException | InterruptedException e) {
              return false;
            }
          });
    }

    static boolean isMavenPresent(Path projectPath) {
      Path pom = projectPath.resolve("pom.xml");
      if (!Files.exists(pom)) {
        return false;
      }

      try {
        // This is hokey compared to parsing the pom, but ...
        return Files.readAllLines(pom).stream().anyMatch((line) -> line.contains("appmap-maven-plugin"));
      } catch (IOException e) {
        return false;
      }
    }

    static boolean isMavenValid(Path projectPath) {
      try {
        // If this works, things are looking pretty good.
        String command = "mvn prepare-agent";
        // System.err.printf("Attempting: %s\n", command);
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor(60, TimeUnit.SECONDS);
        return process.exitValue() == 0;
      } catch (IOException | InterruptedException e) {
        return false;
      }
    }
  }

  static class Result {
    public List<TestCommand> testCommands = new ArrayList<>();
    public Status.Properties properties = new Status.Properties();
  }

  public Integer call() {
    System.err.printf("Reporting AppMap project status in directory: %s\n", parent.directory);

    Path dirPath = FileSystems.getDefault().getPath(parent.directory);
    Path configPath = dirPath.resolve("appmap.yml");

    Result result = new Result();
    result.properties.config.app = CLI.projectName(dirPath.toFile());
    result.properties.config.present = Files.exists(configPath);
    if (result.properties.config.present) {
      result.properties.config.valid = Properties.isConfigValid(configPath);
    }
    FrameworkStatus gradleStatus = new FrameworkStatus();
    gradleStatus.name = "gradle";
    gradleStatus.present = Properties.isGradlePresent(dirPath);
    if (gradleStatus.present) {
      gradleStatus.valid = Properties.isGradleValid(dirPath);
    }
    result.properties.frameworks.add(gradleStatus);

    FrameworkStatus mavenStatus = new FrameworkStatus();
    mavenStatus.name = "maven";
    mavenStatus.present = Properties.isMavenPresent(dirPath);
    if (mavenStatus.present) {
      mavenStatus.valid = Properties.isMavenValid(dirPath);
    }

    if (gradleStatus.valid) {

      String gradleWrapper = SystemUtils.IS_OS_WINDOWS ? "gradlew.bat" : "./gradlew";

      Command command = new Command();
      command.program = gradleWrapper;
      command.args.add("appmap");
      command.args.add("test");

      TestCommand tc = new TestCommand("gradle");
      tc.command = command;

      result.testCommands.add(tc);
    }
    if (mavenStatus.valid) {
      String mavenWrapper = SystemUtils.IS_OS_WINDOWS ? "mvnw.cmd" : "./mvnw";
      if ( !new File(mavenWrapper).exists() ) {
        mavenWrapper = "mvn";
      }

      Command command = new Command();
      command.program = mavenWrapper;
      command.args.add("test");

      TestCommand tc = new TestCommand("maven");
      tc.command = command;

      result.testCommands.add(tc);
    }

    System.out.println(JSON.toJSONString(result, SerializerFeature.PrettyFormat));

    return 0;
  }
}