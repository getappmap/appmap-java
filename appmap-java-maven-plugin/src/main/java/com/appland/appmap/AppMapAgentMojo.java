package com.appland.appmap;

import com.appland.shade.org.apache.commons.lang3.StringEscapeUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class AppMapAgentMojo extends AbstractMojo {

    static final String APPMAP_AGENT_ARTIFACT_NAME = "com.appland.appmap:java-agent";
    static final String SUREFIRE_ARG_LINE = "argLine";

    @Parameter(property = "skip")
    protected boolean skip = false;

    @Parameter(property = "project.outputDirectory")
    protected File outputDirectory = new File("tmp");

    @Parameter(property = "project.configFile")
    protected File configFile = new File("appmap.yml");

    @Parameter(property = "project.debug")
    protected String debug = "disabled";

    @Parameter(property = "project.eventValueSize")
    protected Integer eventValueSize = 1024;

    @Parameter(property = "plugin.artifactMap")
    protected Map<String, Artifact> pluginArtifactMap;

    @Parameter(property = "project")
    private MavenProject project;

    public abstract void execute() throws MojoExecutionException;

    protected void skipMojo() {
    }

    protected void loadAppMapJavaAgent() {
        final String newValue = buildArguments();
        setProjectArgLineProperty(newValue);
        getLog().info(SUREFIRE_ARG_LINE
                + " set to " + StringEscapeUtils.unescapeJava(newValue));
    }

    /**
     * This method builds the needed parameter to run the Agent, if previous configuration is found is also attached in
     * the SUREFIRE_ARG_LINE, if previous version of the AppMap agent is found is removed and replaced with the version
     * of this maven plugin
     *
     * @return formatted and escaped arguments to run on command line
     */
    private String buildArguments() {
        final String oldConfig = getCurrentArgLinePropertyValue();
        final List<String> oldArgs = Arrays.asList(oldConfig.split(" "));
        removeOldAppMapAgentFromCommandLine(oldArgs);
        List<String> args = new ArrayList<String>();
        addMvnAppMapCommandLineArgs(args);
        args.addAll(oldArgs);
        return args.stream().collect(Collectors.joining(" "));
    }

    private void removeOldAppMapAgentFromCommandLine(List<String> oldArgs) {
        final String plainAgent = format("-javaagent:%s", getAppMapAgentJar());
        for (final Iterator<String> i = oldArgs.iterator(); i.hasNext(); ) {
            if (i.next().startsWith(plainAgent)) {
                i.remove();
            }
        }
    }

    private void addMvnAppMapCommandLineArgs(List<String> args) {
        args.add(StringEscapeUtils.escapeJava(
                format("-javaagent:%s=%s", getAppMapAgentJar(), this)
        ));
        args.add("-Dappmap.debug=" + StringEscapeUtils.escapeJava(debug));
        args.add("-Dappmap.output.directory=" + StringEscapeUtils.escapeJava(format("%s", outputDirectory)));
        args.add("-Dappmap.config.file=" + StringEscapeUtils.escapeJava(format("%s", configFile)));
        args.add("-Dappmap.event.valueSize=" + eventValueSize);
    }


    private Object setProjectArgLineProperty(String newValue) {
        return project.getProperties().setProperty(SUREFIRE_ARG_LINE, newValue);
    }

    private String getCurrentArgLinePropertyValue() {
        return project.getProperties().getProperty(SUREFIRE_ARG_LINE);
    }

    protected File getAppMapAgentJar() {
        return pluginArtifactMap.get(APPMAP_AGENT_ARTIFACT_NAME).getFile();
    }

    public MavenProject getProject() {
        return project;
    }
}
