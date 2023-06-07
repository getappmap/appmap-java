package com.appland.appmap.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.tinylog.Level;
import org.tinylog.configuration.ConfigurationLoader;
import org.tinylog.provider.InternalLogger;
import org.tinylog.runtime.RuntimeProvider;

public class AppMapTinylogConfigurationLoader implements ConfigurationLoader {

  @Override
  public Properties load() throws IOException {
    Properties properties = new Properties();
    final File localConfigFile = new File("appmap-log.local.properties");
    final String[] configFiles = { "appmap-log.properties", localConfigFile.getName() };

    for (String configFile : configFiles) {
      List<ClassLoader> classLoaders = RuntimeProvider.getClassLoaders();
      for (ClassLoader cl : classLoaders) {
        try (InputStream stream = cl.getResourceAsStream(configFile)) {
          if (stream != null) {
            properties.load(stream);
          }
        }
      }
    }

    try {
      if (localConfigFile.exists()) {
        properties.load(new FileInputStream(localConfigFile));
      }
    } catch (IOException e) {
      InternalLogger.log(Level.ERROR, e, "Failed to load " + localConfigFile.getAbsolutePath());
    }

    // Update with any appmap.log System properties.
    for (Map.Entry<Object, Object> p : System.getProperties().entrySet()) {
      String prefix = "appmap.log.";
      String key = (String) p.getKey();
      if (!key.startsWith(prefix)) {
        continue;
      }

      properties.put(key.substring(prefix.length()), p.getValue());
    }

    return properties;
  }

}