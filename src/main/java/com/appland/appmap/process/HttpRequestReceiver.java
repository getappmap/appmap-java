package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;

import java.io.IOException;
import java.io.PrintWriter;
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
public class HttpRequestReceiver implements IEventProcessor {
  private static final Recorder recorder = Recorder.getInstance();
  private static final HashSet<Long> runningThreads = new HashSet<Long>();

  private Boolean isExecuting = false;

  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  private Boolean startExecuting() {
    Long threadId = Thread.currentThread().getId();
    Boolean nobodyExecuting = HttpRequestReceiver.runningThreads.contains(threadId) == false;
    if (nobodyExecuting) {
      HttpRequestReceiver.runningThreads.add(threadId);
      this.isExecuting = true;
    }
    return nobodyExecuting;
  }

  private Boolean stopExecuting() {
    if (!this.isExecuting) {
      return false;
    }

    Long threadId = Thread.currentThread().getId();
    HttpRequestReceiver.runningThreads.remove(threadId);
    return true;
  }

  private Boolean isExecuting() {
    return this.isExecuting;
  }

  private void invokeDoFilter(Event event,
                              HttpServletRequest req,
                              HttpServletResponse res,
                              FilterChain chain) {
    event.setParameters(null);
    event.setHttpServerRequest(req.getMethod(), req.getRequestURI(), req.getProtocol());

    recorder.add(event);
  }

  private void returnDoFilter(Event event,
                              HttpServletRequest req,
                              HttpServletResponse res,
                              FilterChain chain) {
    event.setParameters(null);
    event.setHttpServerResponse(200);

    recorder.add(event);
  }

  @Override
  public Boolean onEnter(Event event) {
    if (!this.startExecuting()) {
      return true;
    }

    try {
      this.request = event.getParameter(0).get();
      this.response = event.getParameter(1).get();
      this.filterChain = event.getParameter(2).get();
    } catch (IllegalArgumentException e) {
      System.err.println("AppMap: failed to get parameter");
      System.err.println(e.getMessage());
      return true;
    }

    event.setParameters(null);
    event.setHttpServerRequest(this.request.getMethod(),
        this.request.getRequestURI(),
        this.request.getProtocol());

    recorder.add(event);
    return true;
  }

  @Override
  public void onExit(Event event) {
    if (!this.stopExecuting()) {
      return;
    }

    if (this.response == null) {
      return;
    }

    HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(this.response);
    event.setHttpServerResponse(responseWrapper.getStatus(), responseWrapper.getContentType());

    recorder.add(event);
  }
}
