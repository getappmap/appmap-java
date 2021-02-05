package com.appland.appmap;

import com.appland.shade.org.apache.commons.lang3.StringEscapeUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Parameter(property = "project")
    protected MavenProject project;

    @Parameter(property = "plugin.artifactMap")
    protected Map<String, Artifact> pluginArtifactMap;

    public abstract void execute() throws MojoExecutionException;

    protected void skipMojo() {
    }

    protected void loadAppMapJavaAgent() {
        final String newValue = buildArguments();
        setProjectArgLineProperty(newValue);
        getLog().info(SUREFIRE_ARG_LINE
                + " set to " + StringEscapeUtils.unescapeJava(newValue));
    }

    private String buildArguments() {
        List<String> args = new ArrayList<String>();
        args.add(StringEscapeUtils.escapeJava(
                format("-javaagent:%s=%s", getAppMapAgentJar(), this)
        ));
        args.add("-Dappmap.debug=" + StringEscapeUtils.escapeJava(debug));
        args.add("-Dappmap.output.directory=" + StringEscapeUtils.escapeJava( format("%s",outputDirectory)));
        args.add("-Dappmap.config.file=" + StringEscapeUtils.escapeJava( format("%s",configFile)));
        args.add("-Dappmap.event.valueSize=" + eventValueSize);
        return args.stream().collect(Collectors.joining(" ")).toString();
    }


    private Object setProjectArgLineProperty(String newValue) {
        return project.getProperties().setProperty(SUREFIRE_ARG_LINE, newValue);
    }

    protected File getAppMapAgentJar() {
        return pluginArtifactMap.get(APPMAP_AGENT_ARTIFACT_NAME).getFile();
    }
}
