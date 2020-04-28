package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public interface IRecordingSession {
  class Metadata {
    public String scenarioName;
    public String recorderName;
    public String framework;
    public String frameworkVersion;
    public String recordedClassName;
    public String recordedMethodName;
    public String feature;
    public String featureGroup;
  }

  public void add(Event event) throws ActiveSessionException;

  public void add(CodeObject codeObject) throws ActiveSessionException;

  public void start() throws ActiveSessionException;

  public String stop() throws ActiveSessionException;
}
