package com.appland.appmap.cli;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
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

  static class ValidationResult {
    public final Integer version = 2;
    public ArrayList<Error> errors;
    public Map<?, ?> schema;
    ValidationResult(ArrayList<Error> errors, Map<?, ?>schema) {
      this.errors = errors;
      this.schema = schema;
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

    ArrayList<Error> errors = new ArrayList<Error>();

    Error nextError;
    if ((nextError = checkVersion()) != null) {
      errors.add(nextError);
    }

    final ClassLoader cl = getClass().getClassLoader();
    final InputStream schemaStream = cl.getResourceAsStream("config-schema.yml");
    final Yaml yaml = new Yaml();
    final Map<?, ?> schema = (Map<?, ?>)yaml.load(schemaStream);

    final ValidationResult result = new ValidationResult(errors, schema);

    parent.getOutputStream().println(JSON.toJSONString(result, SerializerFeature.PrettyFormat).replace("\t", "  "));

    // Let the CLI decide whether validation failed.
    return 0;
  }
}
