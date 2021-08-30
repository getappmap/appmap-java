package com.appland.appmap.cli;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "validate", description = "Validates that a Java project is ready to create AppMaps.")
public class Validate implements Callable<Integer> {
  @CommandLine.ParentCommand
  private CLI parent;

  static class Error {
    public final String level = "error";
    public String message;
    public String detailed_message;
    Error(String message) {
      this.message = message;
    }
  }

  private Error checkVersion() {
    Error ret = null;
    if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8)) {
      ret = new Error("Unsupported Java Version " + SystemUtils.JAVA_VERSION);
    }
    return ret;
  }

  public Integer call() {
    System.err.printf("Validating AppMap project in directory: %s\n", parent.directory);

    ArrayList<Error> result = new ArrayList<Error>();

    Error nextError;
    if ((nextError = checkVersion()) != null) {
      result.add(nextError);
    }

    parent.getOutputStream().println(JSON.toJSONString(result, SerializerFeature.PrettyFormat));

    return result.size() == 0?  0 : 1;
  }
}