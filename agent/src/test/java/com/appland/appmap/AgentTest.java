package com.appland.appmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

public class AgentTest {

  @Test
  void filterAppMapPropertiesKeepsOnlyAppMapKeys() {
    Properties props = new Properties();
    props.setProperty("appmap.debug", "true");
    props.setProperty("appmap.output.directory", "/tmp/appmap");
    props.setProperty("password", "hunter2");
    props.setProperty("user.home", "/home/user");
    props.setProperty("javax.net.ssl.trustStorePassword", "secret");

    Properties filtered = Agent.filterAppMapProperties(props);

    assertEquals(2, filtered.size());
    assertEquals("true", filtered.getProperty("appmap.debug"));
    assertEquals("/tmp/appmap", filtered.getProperty("appmap.output.directory"));
    assertFalse(filtered.containsKey("password"));
    assertFalse(filtered.containsKey("user.home"));
    assertFalse(filtered.containsKey("javax.net.ssl.trustStorePassword"));
  }

  @Test
  void filterAppMapPropertiesReturnsEmptyWhenNoneMatch() {
    Properties props = new Properties();
    props.setProperty("password", "hunter2");

    Properties filtered = Agent.filterAppMapProperties(props);

    assertTrue(filtered.isEmpty());
  }
}
