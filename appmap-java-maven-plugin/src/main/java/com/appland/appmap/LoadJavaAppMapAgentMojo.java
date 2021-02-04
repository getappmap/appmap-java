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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * Goal which adds appmap.jar to JVM execution as javaagent, right before the test execution begins.
 *
 * @goal prepare-agent
 * @phase test-compile
 */
public class LoadJavaAppMapAgentMojo extends AppMapAgentMojo {

    @Override
    public void execute()
            throws MojoExecutionException {
        getLog().info("Initializing AppLand AppMap Java Recorder  >" + project + "< " + skip + "  " + outputDirectory + "  " + configFile + "   " + debug + " " + eventValueSize);
        try {
            if (skip) {
                getLog().info(
                        "Skipping AppMap execution because property skip is set.");
                skipMojo();
                return;
            } else {
                loadAppMapJavaAgent();
            }
        } catch (Exception e) {
            getLog().error("Error initializing AppLand AppMap Java Recorder");
            e.printStackTrace();
        }
    }

}
