package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ThreadLock;

/**
 * Recorder is a singleton responsible for managing recording sessions and routing events to any
 * active session. It also maintains a code object tree containing every known package/class/method.
 */
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
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: failed to start recording\n%s\n", e.getMessage());
      this.stop();
    }
  }

  /**
   * Get the global Recorder instance.
   * @return The global recorder instance
   */
  public static Recorder getInstance() {
    return Recorder.instance;
  }

  /**
   * Checks whether or not the Recorder has an active recording session.
   * @return {@code true} If a session is in progress. Otherwise, {@code false}.
   */
  public synchronized Boolean hasActiveSession() {
    return this.activeSession != null;
  }

  /**
   * Start a recording session, writing the output to a file.
   * @param fileName Destination file
   * @param metadata Recording metadata to be written
   * @throws ActiveSessionException If a session is already in progress
   */
  public synchronized void start(String fileName, IRecordingSession.Metadata metadata)
      throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionFileStream(fileName, metadata));
  }

  /**
   * Start a recording session, storing recording data in memory.
   * @param metadata Recording metadata to be written
   * @throws ActiveSessionException If a recording session is already in progress
   */
  public synchronized void start(IRecordingSession.Metadata metadata)
      throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionMemory(metadata));
  }

  /**
   * Stops the active recording session.
   * @return Output from the current session. This will be empty unless recording to memory.
   * @throws ActiveSessionException If no recording session is in progress or the session cannot be
   *                                stopped.
   */
  public synchronized String stop() throws ActiveSessionException {
    if (!this.hasActiveSession()) {
      throw new ActiveSessionException(ERROR_NO_SESSION);
    }

    String output = "";

    ThreadLock processorStack = ThreadLock.current();
    try {
      processorStack.enter();
      processorStack.lock();
      output = this.activeSession.stop();
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: failed to stop recording\n%s\n", e.getMessage());
    } finally {
      processorStack.unlock();
      processorStack.exit();
    }

    this.activeSession = null;

    return output;
  }

  /**
   * Record an {@link Event} to the active session.
   * @param event The event to be recorded.
   */
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

  /**
   * Register a {@link CodeObject}, allowing it to propagate to an output's Class Map if referenced
   * in an event.
   * @param codeObject The code object to be registered
   */
  public synchronized void register(CodeObject codeObject) {
    this.globalCodeObjects.add(codeObject);
  }
}
