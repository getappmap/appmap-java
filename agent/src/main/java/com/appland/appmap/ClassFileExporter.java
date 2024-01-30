package com.appland.appmap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

public class ClassFileExporter extends AgentBuilder.Listener.Adapter {

  private final Path folder;

  public ClassFileExporter(Path folder) {
    this.folder = folder;
  }

  @Override
  public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
      JavaModule module, boolean loaded, DynamicType dynamicType) {
    try {
      Path outputFile = folder.resolve(typeDescription.getName().replace('.', '/') + ".class");
      Files.createDirectories(outputFile.getParent());
      try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
        fos.write(dynamicType.getBytes());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
