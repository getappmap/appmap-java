package com.appland.appmap.record;

import com.appland.appmap.output.v1.Event;

public class ActiveSession {
  private RecordingSession activeSession = null;

  synchronized RecordingSession get() throws ActiveSessionException {
    if (activeSession == null) {
      throw new ActiveSessionException(Recorder.ERROR_NO_SESSION);
    }

    return activeSession;
  }

  boolean exists() {
    return activeSession != null;
  }

  synchronized RecordingSession release() throws ActiveSessionException {
    if (activeSession == null) {
      throw new ActiveSessionException(Recorder.ERROR_NO_SESSION);
    }

    RecordingSession result = activeSession;
    activeSession = null;
    return result;
  }

  synchronized void set(RecordingSession session) throws ActiveSessionException {
    if (activeSession != null) {
      throw new ActiveSessionException(Recorder.ERROR_SESSION_PRESENT);
    }

    activeSession = session;
  }

  synchronized void addEvent(Event event) {
    if (activeSession != null) {
      activeSession.add(event);
    }
  }

  synchronized void addEventUpdate(Event event) {
    if (activeSession != null) {
      activeSession.addEventUpdate(event);
    }
  }
}