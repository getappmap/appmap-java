package com.appland.appmap.classloading;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Runner {
  public static void main(String[] argv) {
    String className = Runner.class.getPackage().getName() + "." + argv[0];
    try {
      TestClass testClass = (TestClass)Class.forName(className).getConstructor().newInstance();
      
      int status = testClass.beforeTest();
      if (status != 0) {
        System.exit(status);
      }

      String[] jars = System.getProperty("testJars").split(";");
      List<URL> urls = new ArrayList<>();
      for (String jar : jars) {
        Path jarPath = Paths.get(jar);
        urls.add(jarPath.toUri().toURL());
      }
      URLClassLoader cl = URLClassLoader.newInstance(urls.toArray(new URL[0]));
      Thread.currentThread().setContextClassLoader(cl);

      status = testClass.runTest();
      System.exit(status);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.exit(1);
  }
}
