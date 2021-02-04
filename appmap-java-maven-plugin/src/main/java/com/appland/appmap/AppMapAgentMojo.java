package com.appland.appmap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;

public abstract class AppMapAgentMojo extends AbstractMojo {
    /**
     * Name of the AppMap Agent artifact.
     */
    static final String AGENT_ARTIFACT_NAME = "com.appland.appmap:java-agent";
    /**
     * Name of the property used in maven-surefire-plugin.
     */
    static final String SUREFIRE_ARG_LINE = "argLine";
    /**
     * Name of the property used in maven-osgi-test-plugin.
     */
    static final String TYCHO_ARG_LINE = "tycho.testArgLine";
    /**
     * Directory where reports will be generated. @default /tmp
     *
     * @parameter expression="${project.outputDirectory}"
     */
    protected File outputDirectory;
    /**
     * Configuration file for the java agent @default /appmap.yml.
     *
     * @parameter expression="${project.configFile}"
     */
    protected File configFile;
    /**
     * Java Agent Debug Option.
     *
     * @parameter expression="${project.debug}
     */
    protected String debug = "disabled";
    /**
     * Java Agent Event Description Size.
     *
     * @parameter expression="${project.eventValueSize}
     */
    protected Integer eventValueSize = 1024;
    /**
     * Maven project.
     *
     * @parameter expression="${project}
     */
    protected MavenProject project;
    /**
     * Flag used to suppress execution.
     *
     * @parameter expression="${project.skip}
     */
    protected boolean skip;
    /**
     * Allows to specify property which will contains settings for AppMap Agent.
     * If not specified, then "argLine" would be used for "jar" packaging and
     * "tycho.testArgLine" for "eclipse-test-plugin".
     */
    @Parameter(property = "appmap.propertyName")
    String propertyName;

    public abstract void execute()
            throws MojoExecutionException;

    protected void skipMojo() {
    }

    /**
     * @return Maven project
     */
    protected final MavenProject getProject() {
        return project;
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

    protected boolean isPropertyNameSpecified() {
        return propertyName != null && !"".equals(propertyName);
    }

    protected boolean isEclipseTestPluginPackaging() {
        return "eclipse-test-plugin".equals(getProject().getPackaging());
    }

    protected String prependVMArguments(final String arguments,
                                        final File agentJarFile) {
        final List<String> args = CommandLineSupport.split(arguments);
        final String plainAgent = format("-javaagent:%s", agentJarFile);
        for (final Iterator<String> i = args.iterator(); i.hasNext(); ) {
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
     * @param agentJarFile location of the AppMap Agent Jar
     * @return Argument to pass to create new VM with App Mapping enabled
     */
    protected String getVMArgument(final File agentJarFile) {
        return format("-javaagent:%s=%s", agentJarFile, this);
    }
}
