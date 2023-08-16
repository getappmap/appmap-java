package com.appland.appmap.cli;

import java.nio.file.FileSystems;
import java.util.concurrent.Callable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.appland.appmap.config.AppMapConfig;

import picocli.CommandLine;

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

    AppMapConfig.initialize(FileSystems.getDefault());

    String contents = AppMapConfig.getDefault(parent.directory);

    Configuration configuration = new Configuration();
    configuration.filename = "appmap.yml";
    configuration.contents = contents;
    Result result = new Result();
    result.configuration = configuration;

    parent.getOutputStream().println(JSON.toJSONString(result, SerializerFeature.PrettyFormat));

    return 0;
  }
}
