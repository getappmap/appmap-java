package com.appland.appmap.util;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

public class PomUpdater {
  public static void main(String[] argv) {
    String pomFilePath = argv[0];
    String annotationJarPath = argv[1];

    MavenXpp3Reader reader = new MavenXpp3Reader();
    MavenXpp3Writer writer = new MavenXpp3Writer();

    try {
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

      System.err.println("PomUpdater: updating " + argv[0]);

      addAnnotationJar(annotationJarPath, model);

      addProfiles(model);

      try (FileWriter fileWriter = new FileWriter(pomFilePath)) {
        writer.write(fileWriter, model);
      }
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.exit(1);
  }

  private static void addProfiles(Model model) {
    // Add profiles to allow the choice of embedded servlet container (Tomcat or Jetty) at runtime.
    // Note that spring-boot-starter-web embeds spring-boot-start-tomcat and will always use it if
    // present, so it must be explicictly excluded to allow the use of Jetty.

    Exclusion tomcatExclusion = new Exclusion();
    tomcatExclusion.setGroupId("org.springframework.boot");
    tomcatExclusion.setArtifactId("spring-boot-starter-tomcat");

    model.getDependencies().stream()
        .filter(d -> d.getArtifactId().equals("spring-boot-starter-web")).findFirst()
        .ifPresent(d -> d.addExclusion(tomcatExclusion));

    Dependency tomcatDependency = new Dependency();
    tomcatDependency.setGroupId("org.springframework.boot");
    tomcatDependency.setArtifactId("spring-boot-starter-tomcat");

    Dependency jettyDependency = new Dependency();
    jettyDependency.setGroupId("org.springframework.boot");
    jettyDependency.setArtifactId("spring-boot-starter-jetty");

    Profile tomcatProfile = new Profile();
    tomcatProfile.setId("tomcat");
    Activation activation = new Activation();
    activation.setActiveByDefault(true);
    tomcatProfile.setActivation(activation);
    tomcatProfile.setDependencies(Arrays.asList(tomcatDependency));

    Profile jettyProfile = new Profile();
    jettyProfile.setId("jetty");
    jettyProfile.setDependencies(Arrays.asList(jettyDependency));

    model.addProfile(tomcatProfile);
    model.addProfile(jettyProfile);
  }

  private static void addAnnotationJar(String annotationJar, Model model) {
    Dependency newDependency = new Dependency();
    newDependency.setGroupId("com.appland");
    newDependency.setArtifactId("annotation");
    newDependency.setVersion("LATEST");
    newDependency.setScope("system");
    newDependency.setSystemPath(annotationJar);

    // Add the new dependency to the model
    model.addDependency(newDependency);
  }
}
