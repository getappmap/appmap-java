package com.appland.appmap;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Goal that adds appmap.jar to JVM execution as javaagent,
 * right before the test execution begins.
 */
@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.TEST_COMPILE)
public class LoadJavaAppMapAgentMojo extends AppMapAgentMojo {

    @Override
    public void execute()
            throws MojoExecutionException {
        try {
            if (skip) {
                getLog().info("Skipping AppLand AppMap execution because property skip is set.");
                skipMojo();
                return;
            } else {
                getLog().info("Initializing AppLand AppMap Java Recorder." );
                loadAppMapJavaAgent();
            }
        } catch (Exception e) {
            getLog().error("Error initializing AppLand AppMap Java Recorder");
            e.printStackTrace();
        }
    }
}
