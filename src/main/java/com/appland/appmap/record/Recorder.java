package com.appland.appmap.record;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.RecordingSession.Metadata;
import com.appland.appmap.util.Logger;

/**
 * Recorder is a singleton responsible for managing recording sessions and routing events to any
 * active session. It also maintains a code object tree containing every known package/class/method.
 */
public class Recorder {
  private static final String ERROR_SESSION_PRESENT = "an active recording session already exists";
  private static final String ERROR_NO_SESSION = "there is no active recording session";

  private RecordingSession activeSession = null;
  private CodeObjectTree globalCodeObjects = new CodeObjectTree();
  private Map<Long, Event> queuedEvents = new HashMap<Long, Event>();

  private static Recorder instance = new Recorder();

  private Recorder() {

  }

  /**
   * Get the global Recorder instance.
   *
   * @return The global recorder instance
   */
  public static Recorder getInstance() {
    return Recorder.instance;
  }

  /**
   * Checks whether or not the Recorder has an active recording session.
   *
   * @return {@code true} If a session is in progress. Otherwise, {@code false}.
   */
  public synchronized Boolean hasActiveSession() {
    return this.activeSession != null;
  }

  public synchronized RecordingSession getActiveSession()
      throws ActiveSessionException {
    if (this.activeSession == null) {
      throw new ActiveSessionException(ERROR_NO_SESSION);
    }

    return this.activeSession;
  }

  /**
   * Start a recording session.
   *
   * @param metadata Recording metadata to be written
   * @throws ActiveSessionException If a session is already in progress
   */
  public synchronized void start(Metadata metadata) throws ActiveSessionException {
    if (this.hasActiveSession()) {
      throw new ActiveSessionException(ERROR_SESSION_PRESENT);
    }

    this.activeSession = new RecordingSession();
    this.activeSession.start(metadata);
  }

  public Recording checkpoint() {
    RecordingSession recordingSession = this.getActiveSession();
    this.flush(recordingSession);
    return recordingSession.checkpoint();
  }

  /**
   * Freeze the event and write it to the active session. Do NOT call this
   * method from within a block locked by `this`. Freezing an event will step
   * into target application code, potentially accessing a locked resource and
   * entering a deadlocked state.
   */
  private void writeEvent(Event event, RecordingSession recordingSession)
      throws ActiveSessionException {
    event.freeze();

    recordingSession.add(event);
  }

  /**
   * Flush all queued events, writing them to the active session and clearing
   * the queue.
   */
  private void flush(RecordingSession recordingSession) throws ActiveSessionException {
    Collection<Event> events;

    synchronized (this) {
      events = new LinkedList<Event>(this.queuedEvents.values());
      this.queuedEvents.clear();
    }

    for (Event event : events) {
      this.writeEvent(event, recordingSession);
    }
  }

  private void queueEvent(Event event) throws ActiveSessionException {
    Event pendingEvent;
    RecordingSession recordingSession;

    synchronized (this) {
      if (this.activeSession == null) {
        return;
      }

      recordingSession = this.activeSession;
      pendingEvent = this.queuedEvents.get(event.threadId);
      this.queuedEvents.put(event.threadId, event);
    }

    if (pendingEvent != null) {
      this.writeEvent(pendingEvent, recordingSession);
    }
  }

  private synchronized void forceStop() {
    if (this.activeSession == null) {
      return;
    }

    Recording recording = this.activeSession.stop();
    recording.delete();
    this.activeSession = null;
  }

  /**
   * Stops the active recording session.
   *
   * @return Output from the current session. This will be empty unless recording to memory.
   * @throws ActiveSessionException If no recording session is in progress or the session cannot be
   *                                stopped.
   */
  public Recording stop() throws ActiveSessionException {
    RecordingSession recordingSession;

    synchronized (this) {
      recordingSession = this.getActiveSession();
      this.activeSession = null;
    }

    try {
      this.flush(recordingSession);
      return recordingSession.stop();
    } catch (ActiveSessionException e) {
      Logger.printf("failed to stop recording\n%s\n", e.getMessage());
      this.forceStop();
      return null;
    }
  }

  /**
   * Record an {@link Event} to the active session.
   *
   * @param event The event to be recorded.
   */
  public void add(Event event) {
    try {
      this.queueEvent(event);
    } catch (ActiveSessionException e) {
      Logger.println("failed to record event");
      Logger.println(e);
      this.forceStop();
    }
  }

  /**
   * Register a {@link CodeObject}, allowing it to propagate to an output's Class Map if referenced
   * in an event.
   *
   * @param codeObject The code object to be registered
   */
  public synchronized void register(CodeObject codeObject) {
    this.globalCodeObjects.add(codeObject);
  }

  public CodeObjectTree getRegisteredObjects() {
    return this.globalCodeObjects;
  }

  /**
   * Retrieve the last event recorded.
   */
  public synchronized Event getLastEvent() {
    final Long threadId = Thread.currentThread().getId();
    return this.queuedEvents.get(threadId);
  }

  /**
   * Record the execution of a Runnable and return the scenario data as a String
   * @return
   */
  public Recording record(Runnable fn) throws ActiveSessionException {
    this.start(new Metadata());
    fn.run();
    return this.stop();
  }

  /**
   * Record the execution of a Runnable and write the scenario to a file
   */
  public void record(String name, Runnable fn) throws ActiveSessionException, IOException {
    final String fileName = name.replaceAll("[^a-zA-Z0-9-_]", "_");
    final Metadata metadata = new Metadata();
    metadata.scenarioName = name;

    this.start(metadata);
    fn.run();
    Recording recording = this.stop();
    recording.moveTo(fileName + ".appmap.json");
  }
}
