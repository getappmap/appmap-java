package com.appland.appmap.test.fixture;

import com.appland.appmap.Agent;
import com.appland.appmap.config.Properties;

/**
 * Test that verifies the agent can run when loaded on the bootstrap classpath.
 * This is a regression test for the fix that prevents NullPointerException when
 * Agent.class.getClassLoader() returns null.
 */
public class TestBootstrapClasspath implements TestClass {

  @Override
  public int beforeTest() throws Exception {
    return 0;
  }

  @Override
  public int runTest() throws Exception {
    // Verify that the agent is actually running on the bootstrap classpath
    ClassLoader agentClassLoader = Agent.class.getClassLoader();
    if (agentClassLoader != null) {
      System.err.println("ERROR: Agent is not running on bootstrap classpath");
      System.err.println("Agent class loader: " + agentClassLoader);
      return 1;
    }

    // Verify that Properties class is also on bootstrap classpath
    ClassLoader propertiesClassLoader = Properties.class.getClassLoader();
    if (propertiesClassLoader != null) {
      System.err.println("ERROR: Properties is not running on bootstrap classpath");
      System.err.println("Properties class loader: " + propertiesClassLoader);
      return 1;
    }

    // Verify that Git integration is automatically disabled when on bootstrap classpath
    if (!Properties.DisableGit) {
      System.err.println("ERROR: Git integration should be automatically disabled on bootstrap classpath");
      return 1;
    }

    System.out.println("SUCCESS: Agent running on bootstrap classpath with Git disabled");
    return 0;
  }
}
