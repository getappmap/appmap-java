package com.appland.appmap;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;

/**
 * Goal which adds appmap.jar to JVM execution as javaagent, right before the test execution begins.
 *
 * @goal prepare-agent
 * @phase test-compile
 */
public class LoadJavaAppMapAgentMojo extends AbstractMojo {
    /**
     * Directory where reports will be generated. @default /tmp
     *
     * @parameter expression="${project.outputDirectory}"
     *
     */
    private File outputDirectory;

    /**
     * Configuration file for the java agent @default /appmap.yml.
     *
     * @parameter expression="${project.configFile}"
     *
     */
    private File configFile;

    /**
     * Java Agent Debug Option.
     *
     * @parameter expression="${project.debug}
     *
     */
    private String debug = "disabled";

    /**
     * Java Agent Event Description Size.
     *
     * @parameter expression="${project.eventValueSize}
     *
     */
    private Integer eventValueSize = 1024;

    /**
     * Maven project.
     * @parameter expression="${project}
     */
    private MavenProject project;

    /**
     * Flag used to suppress execution.
     *
     * @parameter expression="${project.skip}
     */
    private boolean skip;

    /**
     * Name of the AppMap Agent artifact.
     */
    static final String AGENT_ARTIFACT_NAME = "com.appland.appmap:java-agent";

    /**
     * Map of plugin artifacts.
     * @parameter expression="${plugin.artifactMap}
     */
    //@Parameter(property = "plugin.artifactMap", required = true, readonly = true)
    Map<String, Artifact> pluginArtifactMap;

    /**
     * Allows to specify property which will contains settings for AppMap Agent.
     * If not specified, then "argLine" would be used for "jar" packaging and
     * "tycho.testArgLine" for "eclipse-test-plugin".
     */
    @Parameter(property = "appmap.propertyName")
    String propertyName;

    /**
     * Name of the property used in maven-surefire-plugin.
     */
    static final String SUREFIRE_ARG_LINE = "argLine";

    /**
     * Name of the property used in maven-osgi-test-plugin.
     */
    static final String TYCHO_ARG_LINE = "tycho.testArgLine";


    public void execute()
            throws MojoExecutionException {
        getLog().info("Initializing AppLand AppMap Java Recorder  >" + project + "< " + skip+ "  "+outputDirectory + "  "+ configFile + "   " + debug + " "+ eventValueSize);
        try {
            if (skip) {
                getLog().info(
                        "Skipping AppMap execution because property skip is set.");
                skipMojo();
                return;
            }else {
                final String name = getEffectivePropertyName();
                final Properties projectProperties = getProject().getProperties();
                final String oldValue = projectProperties.getProperty(name);

                final String newValue = prependVMArguments(oldValue, getAgentJarFile());
                getLog().info(name + " set to " + newValue);
                projectProperties.setProperty(name, newValue);
            }


        }catch (Exception e) {
            getLog().error("Error initializing AppLand AppMap Java Recorder");
            e.printStackTrace();
        }
    }

    /**
     * Skips Mojo.
     */
    protected void skipMojo() {
    }

    /**
     * @return Maven project
     */
    protected final MavenProject getProject() {
        return project;
    }

    File getAgentJarFile() {
        final Artifact appmapAgentArtifact = pluginArtifactMap.get(AGENT_ARTIFACT_NAME);
        return appmapAgentArtifact.getFile();
    }

    String getEffectivePropertyName() {
        if (isPropertyNameSpecified()) {
            return propertyName;
        }
        if (isEclipseTestPluginPackaging()) {
            return TYCHO_ARG_LINE;
        }
        return SUREFIRE_ARG_LINE;
    }

    boolean isPropertyNameSpecified() {
        return propertyName != null && !"".equals(propertyName);
    }

    boolean isEclipseTestPluginPackaging() {
        return "eclipse-test-plugin".equals(getProject().getPackaging());
    }

    public String prependVMArguments(final String arguments,
                                     final File agentJarFile) {
        final List<String> args = CommandLineSupport.split(arguments);
        final String plainAgent = format("-javaagent:%s", agentJarFile);
        for (final Iterator<String> i = args.iterator(); i.hasNext();) {
            if (i.next().startsWith(plainAgent)) {
                i.remove();
            }
        }
        args.add(0, getVMArgument(agentJarFile));
        return CommandLineSupport.quote(args);
    }

    /**
     * Generate required JVM argument based on current configuration and
     * supplied agent jar location.
     *
     * @param agentJarFile
     *            location of the AppMap Agent Jar
     * @return Argument to pass to create new VM with App Mapping enabled
     */
    public String getVMArgument(final File agentJarFile) {
        return format("-javaagent:%s=%s", agentJarFile, this);
    }
}
