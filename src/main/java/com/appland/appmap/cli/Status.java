package com.appland.appmap.cli;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "status", description = "Prints AppMap status of the Java project in a specified directory.")
public class Status implements Callable<Integer> {
  @CommandLine.ParentCommand
  private CLI parent;

  static class Properties {
    @JSONField(name = "config.app")
    public String configApp;
    @JSONField(name = "config.valid")
    public boolean configValid;
    @JSONField(name = "config.present")
    public boolean configPresent;
    @JSONField(name = "agent.version")
    public String agentVersion = Status.class.getPackage().getImplementationVersion();
    @JSONField(name = "project.language")
    public String projectLanguage = "java";
    @JSONField(name = "project.remoteRecordingCapable")
    public boolean projectRemoteRecordingCapable;
    @JSONField(name = "project.integrationTests")
    public boolean projectIntegrationTests;

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
  }

  static class Result {
    @JSONField
    public Status.Properties properties = new Status.Properties();
  }

  public Integer call() {
    System.err.printf("Reporting AppMap project status in directory: %s\n", parent.directory);

    Path configPath = FileSystems.getDefault().getPath(parent.directory, "appmap.yml");

    Result result = new Result();
    result.properties.configApp = CLI.projectName(new File(parent.directory));
    result.properties.configPresent = Files.exists(configPath);
    if (result.properties.configPresent) {
      result.properties.configValid = Properties.isConfigValid(configPath);
    }

    System.out.println(JSON.toJSONString(result, SerializerFeature.PrettyFormat));

    return 0;
  }
}
