package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * HttpRequestReceiver hooks the method <code>javax.servlet.http.HttpServlet#service</code>. If the request
 * route is the remote recording path, the request is hijacked and interpreted as a remote recording command.
 * Otherwise, it's recorded as an appmap event, and processed by the application services.
 *
 * @see recordRoute
 */
public class EventProcessorLock implements IEventProcessor {
  private static final KeyedSet<Long, String> locks = new KeyedSet<Long, String>();
  private Boolean isExecuting = false;

  private Boolean startExecuting() {
    Long threadId = Thread.currentThread().getId();
    String lockKey = this.getLockKey();
    if (locks.add(threadId, lockKey)) {
      this.isExecuting = true;
    }
    return this.isExecuting;
  }

  private Boolean stopExecuting() {
    if (!this.isExecuting) {
      return false;
    }

    Long threadId = Thread.currentThread().getId();
    String lockKey = this.getLockKey();
    return locks.remove(threadId, lockKey);
  }

  protected String getLockKey() {
    return null;
  }

  @Override
  public Boolean onEnter(Event event) {
    if (!this.startExecuting()) {
      return true;
    }

    return this.onEnterLock(event);
  }

  @Override
  public void onExit(Event event) {
    if (!this.stopExecuting()) {
      return;
    }

    onExitLock(event);
  }

  public Boolean onEnterLock(Event event) {
    return true;
  }

  public void onExitLock(Event event) {

  }
}
