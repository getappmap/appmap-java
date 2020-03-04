package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ThreadLock;

public class Recorder {
  private static final String DEFAULT_OUTPUT_DIRECTORY = "./";
  private static final String ERROR_SESSION_PRESENT = "an active recording session already exists";
  private static final String ERROR_NO_SESSION = "there is no active recording session";

  private IRecordingSession activeSession = null;
  private String outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
  private CodeObjectTree globalCodeObjects = new CodeObjectTree();

  private static Recorder instance = new Recorder();

  private Recorder() {
    String outputDirectory = System.getProperty("appmap.output.directory");
    if (outputDirectory != null) {
      this.outputDirectory = outputDirectory;
    }
  }

  private synchronized void setActiveSession(IRecordingSession activeSession)
    throws ActiveSessionException {
    if (this.hasActiveSession()) {
      throw new ActiveSessionException(ERROR_SESSION_PRESENT);
    }

    this.activeSession = activeSession;

    try {
      this.activeSession.start();
    } catch(ActiveSessionException e) {
      System.err.printf("AppMap: failed to start recording\n%s\n", e.getMessage());
      this.stop();
    }
  }

  public static Recorder getInstance() {
    return Recorder.instance;
  }

  public synchronized Boolean hasActiveSession() {
    return this.activeSession != null;
  }

  public synchronized void start(String fileName, IRecordingSession.Metadata metadata) throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionFileStream(fileName, metadata));
  }

  public synchronized void start(IRecordingSession.Metadata metadata) throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionMemory(metadata));
  }

  public synchronized String stop() throws ActiveSessionException {
    if (!this.hasActiveSession()) {
      throw new ActiveSessionException(ERROR_NO_SESSION);
    }

    String output = "";

    ThreadLock processorStack = ThreadLock.current();
    try {
      processorStack.setLock(true);
      output = this.activeSession.stop();
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: failed to stop recording\n%s\n", e.getMessage());
    } finally {
      processorStack.setLock(false);
    }

    this.activeSession = null;

    return output;
  }

  public synchronized void add(Event event) {
    if (!this.hasActiveSession()) {
      return;
    }

    try {
      event.freeze();
      this.activeSession.add(event);

      CodeObject rootObject = this.globalCodeObjects.getMethodBranch(event.definedClass,
          event.methodId,
          event.isStatic,
          event.lineNumber);

      if (rootObject != null) {
        this.add(rootObject);
      }
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: failed to record event\n%s\n", e.getMessage());
      this.activeSession.stop();
    }
  }

  private synchronized void add(CodeObject codeObject) {
    if (!this.hasActiveSession()) {
      return;
    }

    try {
      this.activeSession.add(codeObject);
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: failed to record code object\n%s\n", e.getMessage());
      this.activeSession.stop();
    }
  }

  public synchronized void register(CodeObject codeObject) {
    this.globalCodeObjects.add(codeObject);
  }
}