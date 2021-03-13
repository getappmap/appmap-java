package com.appland.appmap.record;

import java.io.IOException;
import java.util.*;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.IRecordingSession.Metadata;
import com.appland.appmap.util.Logger;

import static com.appland.appmap.util.EventUtil.*;

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
  private Stack<Event> callStack = new Stack<>();

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
      Logger.printf("failed to start recording", e.getMessage());
      Logger.println(e);

      this.stop();
      throw e;
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

  public synchronized IRecordingSession getActiveSession()
      throws ActiveSessionException {
    if (this.activeSession == null) {
      throw new ActiveSessionException(ERROR_NO_SESSION);
    }

    return this.activeSession;
  }

  /**
   * Start a recording session, writing the output to a file.
   * @param fileName Destination file
   * @param metadata Recording metadata to be written
   * @throws ActiveSessionException If a session is already in progress
   */
  public synchronized void start(String fileName, Metadata metadata)
      throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionFileStream(fileName, metadata));
  }

  /**
   * Start a recording session, storing recording data in memory.
   * @param metadata Recording metadata to be written
   * @throws ActiveSessionException If a recording session is already in progress
   */
  public synchronized void start(Metadata metadata)
      throws ActiveSessionException {
    this.setActiveSession(new RecordingSessionMemory(metadata));
  }

  /**
   * Freeze the event and write it to the active session. Do NOT call this
   * method from within a block locked by `this`. Freezing an event will step
   * into target application code, potentially accessing a locked resource and
   * entering a deadlocked state.
   */
  private void writeEvent(Event event, IRecordingSession recordingSession)
      throws ActiveSessionException {
    event.freeze();

    recordingSession.add(event);
  }

  /**
   * Flush all queued events, writing them to the active session and clearing
   * the queue.
   */
  private void flush(IRecordingSession recordingSession) throws ActiveSessionException {
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
    IRecordingSession recordingSession;

    synchronized (this) {
      if (this.activeSession == null) {
        return;
      }

      recordingSession = this.activeSession;
      pendingEvent = this.queuedEvents.get(event.threadId);


      if (isReturnEvent(event)) {
            Event lastEventInStack = callStack.peek();
            if (lastEventInStack.isParentEventOf(event) ) {
              callStack.pop();
              removeUnnecessaryInfoForReturnEvents(event);
              event.setParentId(lastEventInStack.id);
            } else {
              callStack.push(event);
            }
      } else {
        callStack.push(event);
      }
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

    this.activeSession.stop();
    this.activeSession = null;
  }

  /**
   * Stops the active recording session.
   * @return Output from the current session. This will be empty unless recording to memory.
   * @throws ActiveSessionException If no recording session is in progress or the session cannot be
   *                                stopped.
   */
  public String stop() throws ActiveSessionException {
    IRecordingSession recordingSession;

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
      return "";
    }
  }

  /**
   * Record an {@link Event} to the active session.
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
   */
  public String record(Runnable fn) throws ActiveSessionException {
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

    this.start(fileName, metadata);
    fn.run();
    this.stop();
  }
}
