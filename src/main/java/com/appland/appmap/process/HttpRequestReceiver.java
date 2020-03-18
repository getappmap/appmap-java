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
public class HttpRequestReceiver extends EventProcessorLock {
  private static final Recorder recorder = Recorder.getInstance();

  private HttpServletRequest request;
  private HttpServletResponse response;

  @Override
  protected String getLockKey() {
    return "http_server_request";
  }

  @Override
  public Boolean onEnterLock(Event event) {
    try {
      this.request = event.getParameter(0).get();
      this.response = event.getParameter(1).get();
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
  public void onExitLock(Event event) {
    if (this.response == null) {
      return;
    }

    HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(this.response);
    event.setHttpServerResponse(responseWrapper.getStatus(), responseWrapper.getContentType());

    recorder.add(event);
  }
}
