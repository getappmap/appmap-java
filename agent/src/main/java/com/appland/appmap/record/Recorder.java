package com.appland.appmap.record;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.util.Logger;

/**
 * Keep track of what's going on in the current thread.
 */
class ThreadState {
  // Provides the last event on the current thread, which is used in some cases to
  // update the event post facto.
  private Event lastGlobalEvent;
  private Event lastThreadEvent;

  void setLastGlobalEvent(Event e) {
    lastGlobalEvent = e;
  }

  Event getLastGlobalEvent() {
    return lastGlobalEvent;
  }

  void setLastThreadEvent(Event e) {
    lastThreadEvent = e;
  }

  Event getLastThreadEvent() {
    return lastThreadEvent;
  }

  // Avoid accepting new events on a thread that's already processing an event.
  boolean isProcessing;
  Stack<Event> callStack = new Stack<>();
}
/**
 * Recorder is a singleton responsible for managing recording sessions and routing events to any
 * active session. It also maintains a code object tree containing every known package/class/method.
 */
public class Recorder {
  private static final String ERROR_SESSION_PRESENT = "an active recording session already exists";
  private static final String ERROR_NO_SESSION = "there is no active recording session";

  private static final Recorder instance = new Recorder();

  private final ActiveSession activeSession = new ActiveSession();
  private final CodeObjectTree globalCodeObjects = new CodeObjectTree();
  private final Map<Long, ThreadState> threadState = new ConcurrentHashMap<>();

  public static String sanitizeFilename(String filename) {
    return filename.replaceAll("[^a-zA-Z0-9-_]", "_");
  }

  /**
   * Data structure for reporting AppMap metadata.
   * These fields map to the 'metadata' section of the AppMap JSON.
   */
  public static class Metadata {
    public String scenarioName;
    public String recorderName;
    public String recorderType;
    public String framework;
    public String frameworkVersion;
    public String recordedClassName;
    public String recordedMethodName;
    public String sourceLocation;
    public Boolean testSucceeded;
    public String failureMessage;
    public Integer failureLine; // line where failure occurred in sourceLocation

    public Metadata(String recorderName, String recorderType) {
      this.recorderName = recorderName;
      this.recorderType = recorderType;
    }
  }



  static class ActiveSession {
    // All events get added to the global session
    private RecordingSession globalSession = null;

    // Only events for a specific thread will get added to the thread session
    private static final ThreadLocal<RecordingSession> threadSession = new ThreadLocal<RecordingSession>();

    synchronized RecordingSession get() throws ActiveSessionException {
      if (globalSession == null) {
        throw new ActiveSessionException(ERROR_NO_SESSION);
      }

      return globalSession;
    }

    boolean exists() {
      return globalSession != null || threadSession.get() != null;
    }

    synchronized RecordingSession release() throws ActiveSessionException {
      if (globalSession == null) {
        throw new ActiveSessionException(ERROR_NO_SESSION);
      }

      RecordingSession result = globalSession;
      globalSession = null;
      return result;
    }

    synchronized void set(RecordingSession session) throws ActiveSessionException {
      if (globalSession != null) {
        throw new ActiveSessionException(ERROR_SESSION_PRESENT);
      }

      globalSession = session;
    }

    void setThread(RecordingSession session) throws ActiveSessionException {
      if (threadSession.get() != null) {
        throw new ActiveSessionException(ERROR_SESSION_PRESENT);
      }

      threadSession.set(session);
    }

    RecordingSession getThread() throws ActiveSessionException {
      if (threadSession.get() == null) {
        throw new ActiveSessionException(ERROR_NO_SESSION);
      }

      return threadSession.get();
    }

    RecordingSession releaseThread() throws ActiveSessionException {
      RecordingSession ret = getThread();
      threadSession.remove();
      return ret;
    }

    /**
     * Add an event to both the global session and thread's session
     */
    synchronized void addEvent(Event event) {
      addGlobalEvent(event);
      addThreadEvent(event);
    }

    synchronized void addEventUpdate(Event event) {
      if (globalSession != null) {
        globalSession.addEventUpdate(event);
      }
      if (threadSession.get() != null) {
        threadSession.get().addEventUpdate(event);
      }
    }

    synchronized void addGlobalEvent(Event event) {
      if (globalSession != null) {
        globalSession.add(event);
      }
    }

    synchronized void addThreadEvent(Event event) {
      RecordingSession session = threadSession.get();
      if (session != null) {
        session.add(event);
      }
    }
  }

  /**
   * Get the global Recorder instance.
   *
   * @return The global recorder instance
   */
  public static Recorder getInstance() {
    return Recorder.instance;
  }

  private Recorder() {
  }

  /**
   * Start a recording session.
   *
   * @param metadata Recording metadata to be written
   * @throws ActiveSessionException If a session is already in progress
   */
  public void start(Metadata metadata) throws ActiveSessionException {
    RecordingSession session = new RecordingSession(metadata);
    activeSession.set(session);
  }

  public void setThreadSession(RecordingSession session) throws ActiveSessionException {
    activeSession.setThread(session);
  }

  public boolean hasActiveSession() {
    return activeSession.exists();
  }

  public Metadata getMetadata() throws ActiveSessionException {
    return activeSession.get().getMetadata();
  }

  public Recording checkpoint() {
    this.flush();
    return activeSession.get().checkpoint();
  }

  /**
   * Stops the active recording session and obtains the result.
   *
   * @return Recording of the current session.
   * @throws ActiveSessionException If no recording session is in progress or the session cannot be
   *                                stopped.
   */
  public Recording stop() throws ActiveSessionException {
    this.flush();
    return activeSession.release().stop();
  }

  public Recording stopThread() {
    flushThread();
    return activeSession.releaseThread().stop();
  }
  /**
   * Record an {@link Event} to the active session.
   *
   * @param event The event to be recorded.
   */
  public void add(Event event) {
    if (!activeSession.exists()) {
      return;
    }

    ThreadState ts = threadState();

    // We don't want re-entrant events on the same thread.
    if ( ts.isProcessing ) {
      return;
    }

    ts.isProcessing = true;
    try {
      if ( event.event.equals("call") ) {
        if (!ts.callStack.empty() && event.hasPackageName() && AppMapConfig.get().isShallow(event.fqn()) ) {
          Event parent = ts.callStack.peek();
          if ( parent.hasPackageName() && event.packageName().equals(parent.packageName()) ) {
            event.ignore();
          }
        }
        
        event.setStartTime();
        ts.callStack.push(event);
      } else if ( event.event.equals("return") ) {
        if ( ts.callStack.isEmpty() ) {
          Logger.println("Discarding 'return' event because the call stack is empty for this thread");
          return;
        }

        // To whom it may concern:
        //
        // You may be tempted to try and track the caller Event using a local variable in the
        // generated code for each hooked function. It would be cleaner and more reliable than
        // tracking a call stack here. However, due to issues with Javassist and the JVM,
        // I (KEG) was not able to find a way to declare, initialize, set, and pass an Event that would
        // work with exception handling and finally clauses.
        Event caller = ts.callStack.pop();
        event.parentId = caller.id;
        event.threadId = caller.threadId;
        event.measureElapsed(caller);
        // Erase these fields - see comment in Event#functionReturnEvent
        event.definedClass = null;
        event.methodId = null;
        event.isStatic = null;
        if ( caller.ignored() ) {
          event.ignore();
        }
      } else {
        throw new IllegalArgumentException("Event should be 'call' or 'return', got " + event.event);
      }

      Event previousGlobalEvent = ts.getLastGlobalEvent();
      ts.setLastGlobalEvent(event);
      addPreviousEvent(previousGlobalEvent, activeSession::addGlobalEvent);

      Event previousThreadEvent = ts.getLastThreadEvent();
      ts.setLastThreadEvent(event);
      addPreviousEvent(previousThreadEvent, activeSession::addThreadEvent);

    } finally {
      ts.isProcessing = false;
    }
  }

  private void addPreviousEvent(Event previousEvent, Consumer<Event> eventAdder) {
    if (previousEvent == null || previousEvent.ignored()) {
      return;
    }

    previousEvent.freeze();
    eventAdder.accept(previousEvent);
  }

  /**
   * Register a {@link CodeObject}, allowing it to propagate to an output's Class Map if referenced
   * in an event.
   *
   * @param codeObject The code object to be registered
   */
  public void registerCodeObject(CodeObject codeObject) {
    synchronized (globalCodeObjects) {
      globalCodeObjects.add(codeObject);
    }
  }

  public CodeObjectTree getRegisteredObjects() {
    return this.globalCodeObjects;
  }

  /**
   * Retrieve the last event (of any kind) recorded for this thread. Only used
   * for testing.
   */
  Event getLastEvent() {
    return threadState().getLastGlobalEvent();
  }

  /**
   * Record the execution of a Runnable and return the scenario data as a String
   */
  public Recording record(Runnable fn) throws ActiveSessionException {
    this.start(new Metadata("java", "process"));
    fn.run();
    return this.stop();
  }

  /**
   * Record the execution of a Runnable and write the scenario to a file
   */
  public void record(String name, Runnable fn) throws ActiveSessionException, IOException {
    final String fileName = sanitizeFilename(name);
    final Metadata metadata = new Metadata("java", "process");
    metadata.scenarioName = name;

    this.start(metadata);
    fn.run();
    Recording recording = this.stop();
    recording.moveTo(fileName + ".appmap.json");
  }

  // Mockito can't stub methods on the Collection<ThreadState>
  // returned by values(), so return an iterator on it instead.
  //
  // And, make this method package-protected, because Mockito won't
  // stub private methods.
  /* private */ Iterator<ThreadState> getThreadStateIterator() {
    return this.threadState.values().iterator();
  }

  private ThreadState threadState() {
    ThreadState ts = threadState.get(Thread.currentThread().getId());
    if ( ts == null ) {
      threadState.put(Thread.currentThread().getId(), (ts = new ThreadState()));
    }
    return ts;
  }

  // Finish serializing any remaining events. This is necessary because each event is "open"
  // until the next event on the same thread is received.
  private void flush() {
    getThreadStateIterator().forEachRemaining((ts) -> {
      if (ts.getLastGlobalEvent() == null) {
        return;
      }

      ts.isProcessing = true;
      try {
        Event event = ts.getLastGlobalEvent();
        ts.setLastGlobalEvent(null);

        event.freeze();
        activeSession.addGlobalEvent(event);
        event.defrost();
      } finally {
        ts.isProcessing = false;
      }
    });
  }

  private void flushThread() {
    ThreadState ts = threadState();

    if (ts.getLastThreadEvent() == null) {
      return;
    }

    ts.isProcessing = true;
    try {
      Event event = ts.getLastThreadEvent();
      ts.setLastThreadEvent(null);

      event.freeze();
      activeSession.addThreadEvent(event);
      event.defrost();
    } finally {
      ts.isProcessing = false;
    }
  }

  public void addEventUpdate(Event event) {
    if (!activeSession.exists()) {
      return;
    }
    activeSession.addEventUpdate(event);
  }
}
