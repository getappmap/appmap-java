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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;

import java.io.File;

/**
 * Goal which register a appmap java recording agent, right before the test execution begins.
 *
 * @goal run-map-agent
 * @phase test-compile
 */
public class LoadJavaAppMapAgentMojo extends AbstractMojo {
    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    public void execute()
            throws MojoExecutionException {
        getLog().info("Initializing AppLand AppMap Java Recorder");
        try {
            final IRecordingSession.Metadata metadata = new IRecordingSession.Metadata();
            Recorder.getInstance().start(metadata);
        }catch (Exception e) {
            getLog().error("Error initializing AppLand AppMap Java Recorder");
            e.printStackTrace();
        }
    }
}
