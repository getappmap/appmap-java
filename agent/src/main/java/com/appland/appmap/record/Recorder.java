package com.appland.appmap.record;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.util.Logger;

/**
 * Recorder is a singleton responsible for managing recording sessions and routing events to any
 * active session. It also maintains a code object tree containing every known package/class/method.
 */
public class Recorder {
  static final String ERROR_SESSION_PRESENT = "an active recording session already exists";
  static final String ERROR_NO_SESSION = "there is no active recording session";

  private static final Recorder instance = new Recorder();

  private final ActiveSession globalSession = new ActiveSession();
  private ThreadLocal<ActiveSession> perThreadSession = new ThreadLocal<ActiveSession>();
  private final CodeObjectTree globalCodeObjects = new CodeObjectTree();
  private final Map<Long, ThreadState> threadState = new ConcurrentHashMap<>();

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
    public Throwable exception;

    public Metadata(String recorderName, String recorderType) {
      this.recorderName = recorderName;
      this.recorderType = recorderType;
    }
  }

  /**
   * Keep track of what's going on in the current thread.
   */
  static class ThreadState {
    // Provides the last event on the current thread, which is used in some cases to
    // update the event post facto.
    Event lastEvent;
    // Avoid accepting new events on a thread that's already processing an event.
    boolean isProcessing;
    Stack<Event> callStack = new Stack<>();
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

  public void setPerThreadSession(ActiveSession session) {
    perThreadSession.set(session);
  }

  /**
   * Start a recording session.
   *
   * @param metadata Recording metadata to be written
   * @throws ActiveSessionException If a session is already in progress
   */
  public void start(Metadata metadata) throws ActiveSessionException {
    RecordingSession session = new RecordingSession(metadata);
    getCurrentSession().set(session);
  }

  public boolean hasActiveSession() {
    return getCurrentSession().exists();
  }

  public Metadata getMetadata() throws ActiveSessionException {
    return getCurrentSession().get().getMetadata();
  }

  public Recording checkpoint() {
    this.flush();
    return getCurrentSession().get().checkpoint();
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
    return getCurrentSession().release().stop();
  }

  /**
   * Record an {@link Event} to the active session.
   *
   * @param event The event to be recorded.
   */
  public void add(Event event) {
    if (!getCurrentSession().exists()) {
      return;
    }

    ThreadState ts = threadState();

    // We don't want re-entrant events on the same thread.
    if (ts.isProcessing) {
      return;
    }

    ts.isProcessing = true;
    try {
      if (event.event.equals("call")) {
        if (!ts.callStack.empty() && event.hasPackageName() && AppMapConfig.get().isShallow(event.fqn())) {
          Event parent = ts.callStack.peek();
          if (parent.hasPackageName() && event.packageName().equals(parent.packageName())) {
            event.ignore();
          }
        }

        event.setStartTime();
        ts.callStack.push(event);
      } else if (event.event.equals("return")) {
        if (ts.callStack.isEmpty()) {
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
        if (caller.ignored()) {
          event.ignore();
        }
      } else {
        throw new IllegalArgumentException("Event should be 'call' or 'return', got " + event.event);
      }

      Event previousEvent = ts.lastEvent;
      ts.lastEvent = event;

      // The previous event is given until now to get all its properties.
      if (previousEvent != null && !previousEvent.ignored()) {
        // This is the line that can generate re-entrant events, apparently.
        previousEvent.freeze();
        addEvent(previousEvent);
      }
    } finally {
      ts.isProcessing = false;
    }
  }

  public void addEventUpdate(Event event) {
    if (globalSession.exists()) {
      globalSession.addEventUpdate(event);
    }

    ActiveSession perThread = perThreadSession.get();
    if (perThread != null) {
      if (perThread.exists()) {
        perThread.addEventUpdate(event);
      }
    }
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
   * Retrieve the last event (of any kind) recorded for this thread.
   */
  public Event getLastEvent() {
    return threadState().lastEvent;
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
    final String fileName = name.replaceAll("[^a-zA-Z0-9-_]", "_");
    final Metadata metadata = new Metadata("java", "process");
    metadata.scenarioName = name;

    this.start(metadata);
    fn.run();
    Recording recording = this.stop();
    recording.moveTo(fileName + ".appmap.json");
  }

  private ActiveSession getCurrentSession() {
    ActiveSession perThread = perThreadSession.get();
    return perThread != null ? perThread : globalSession;
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
      if (ts.lastEvent == null) {
        return;
      }

      ts.isProcessing = true;
      try {
        Event event = ts.lastEvent;
        ts.lastEvent = null;

        event.freeze();
        addEvent(event);
      } finally {
        ts.isProcessing = false;
      }
    });
  }

  private void addEvent(Event event) {
    globalSession.addEvent(event);
    ActiveSession perThread = perThreadSession.get();
    if (perThread != null) {
      perThread.addEvent(event);
    }
  }

}
