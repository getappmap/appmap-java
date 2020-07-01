package com.appland.appmap.record;

import java.util.HashMap;
import java.util.Map;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ThreadLock;
import com.appland.appmap.util.Logger;

/**
 * Recorder is a singleton responsible for managing recording sessions and routing events to any
 * active session. It also maintains a code object tree containing every known package/class/method.
 */
public class Recorder {
  private static final String ERROR_SESSION_PRESENT = "an active recording session already exists";
  private static final String ERROR_NO_SESSION = "there is no active recording session";

  private IRecordingSession activeSession = null;
  private CodeObjectTree globalCodeObjects = new CodeObjectTree();
  private Map<Long, Event> queuedEvents = new HashMap<Long, Event>();

  private static Recorder instance = new Recorder();

  private Recorder() {

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
      Logger.printf("AppMap: failed to start recording\n%s\n", e.getMessage());
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

  private void writeEvent(Event event) throws ActiveSessionException {
    event.freeze();
    this.activeSession.add(event);

    CodeObject rootObject = this.globalCodeObjects.getMethodBranch(event.definedClass,
        event.methodId,
        event.isStatic,
        event.lineNumber);

    if (rootObject != null) {
      this.add(rootObject);
    }
  }

  /**
   * Flush all queued events, writing them to the active session and clearing
   * the queue.
   */
  private synchronized void flush() {
    try {
      for (Event event : this.queuedEvents.values()) {
        this.writeEvent(event);
      }
    } catch (ActiveSessionException e) {
      Logger.printf("AppMap: failed to record event\n%s\n", e.getMessage());
      this.activeSession.stop();
    }
    
    this.queuedEvents.clear();
  }

  private synchronized void queueEvent(Event event) {
    final Event pendingEvent = this.queuedEvents.get(event.threadId);
    if (pendingEvent != null) {
      try {
        this.writeEvent(pendingEvent);
      } catch (ActiveSessionException e) {
        Logger.printf("AppMap: failed to record event\n%s\n", e.getMessage());
        this.activeSession.stop();
        return;
      }
    }

    this.queuedEvents.put(event.threadId, event);
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

    // make sure there's no queued event waiting to be written
    this.flush();

    String output = "";

    ThreadLock processorStack = ThreadLock.current();
    try {
      processorStack.enter();
      processorStack.lock();
      output = this.activeSession.stop();
    } catch (ActiveSessionException e) {
      Logger.printf("AppMap: failed to stop recording\n%s\n", e.getMessage());
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

    this.queueEvent(event);
  }

  private synchronized void add(CodeObject codeObject) {
    if (!this.hasActiveSession()) {
      return;
    }

    try {
      this.activeSession.add(codeObject);
    } catch (ActiveSessionException e) {
      Logger.printf("AppMap: failed to record code object\n%s\n", e.getMessage());
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

  /**
   * Retrieve the last event recorded.
   */
  public synchronized Event getLastEvent() {
    final Long threadId = Thread.currentThread().getId();
    return this.queuedEvents.get(threadId);
  }
}
