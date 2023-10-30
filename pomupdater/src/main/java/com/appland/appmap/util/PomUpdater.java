package com.appland.appmap.util;

import java.io.FileReader;
import java.io.FileWriter;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

public class PomUpdater {
  public static void main(String[] argv) {
    // Define the path to the pom.xml file
    String pomFilePath = argv[0];
    String annotationJar = argv[1];

    // Initialize MavenXpp3Reader and MavenXpp3Writer
    MavenXpp3Reader reader = new MavenXpp3Reader();
    MavenXpp3Writer writer = new MavenXpp3Writer();

    try {
      // Read the existing pom.xml
      Model model;
      try (FileReader fileReader = new FileReader(pomFilePath)) {
        model = reader.read(fileReader);
      }

      long count = model.getDependencies().stream()
          .filter(d -> d.getGroupId().equals("com.appland") && d.getArtifactId().equals("annotation"))
          .count();

      if (count != 0) {
        System.err.println("PomUpdater: found existing dependency.");
        System.exit(0);
      }

      System.err.println("PomUpdater: adding annotation dependency to " + argv[0]);

      Dependency newDependency = new Dependency();
      newDependency.setGroupId("com.appland");
      newDependency.setArtifactId("annotation");
      newDependency.setVersion("LATEST");
      newDependency.setScope("system");
      newDependency.setSystemPath(annotationJar);

      // Add the new dependency to the model
      model.addDependency(newDependency);

      // Write the modified model back to the pom.xml
      try (FileWriter fileWriter = new FileWriter(pomFilePath)) {
        writer.write(fileWriter, model);
      }
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.exit(1);
  }
}
