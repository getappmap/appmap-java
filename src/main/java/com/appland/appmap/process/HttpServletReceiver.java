package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * HttpServletReceiver hooks the method <code>javax.servlet.http.HttpServlet#service</code>. If the request
 * route is the remote recording path, the request is hijacked and interpreted as a remote recording command.
 * Otherwise, it's recorded as an appmap event, and processed by the application services.
 *
 * @see recordRoute
 */
public class HttpServletReceiver implements IEventProcessor {
  public static final String recordRoute = "/_appmap/record";
  private static final Recorder recorder = Recorder.getInstance();

  private void doDelete(HttpServletRequest req, HttpServletResponse res) {
    try {
      String json = recorder.stop();
      res.setContentType("application/json");
      res.setContentLength(json.length());

      PrintWriter writer = res.getWriter();
      writer.write(json);
      writer.flush();
    } catch(ActiveSessionException e) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (IOException e) {
      System.err.printf("failed to write response: %s\n", e.getMessage());
    }
  }

  private void doGet(HttpServletRequest req, HttpServletResponse res) {
    res.setStatus(HttpServletResponse.SC_OK);

    String responseJson = String.format("{\"enabled\":%b}", recorder.hasActiveSession());
    res.setContentType("application/json");
    res.setContentLength(responseJson.length());

    try {
      PrintWriter writer = res.getWriter();
      writer.write(responseJson);
      writer.flush();
    } catch (IOException e) {
      System.err.printf("failed to write response: %s\n", e.getMessage());
    }
  }

  private void doPost(HttpServletRequest req, HttpServletResponse res) {
    IRecordingSession.Metadata metadata = new IRecordingSession.Metadata();
    metadata.recorderName = "remote_recording";
    try {
      recorder.start("remote_recording", metadata);
    } catch (ActiveSessionException e) {
      res.setStatus(HttpServletResponse.SC_CONFLICT);
    }
  }

  private Boolean handleRequest(Event event) {
    if (event.event.equals("call") == false) {
      return false;
    }

    Value requestValue = event.getParameter(0);
    Value responseValue = event.getParameter(1);
    if (requestValue == null || responseValue == null) {
      return false;
    }

    if ( !(requestValue.get() instanceof HttpServletRequest) ) {
      System.err.println("Servlet request value " + requestValue.get().getClass().getName() + " is not an HttpServletRequest");
      return false;
    }

    HttpServletRequest req = requestValue.get();
    HttpServletResponse res = responseValue.get();

    if (req.getRequestURI().equals(recordRoute) == false) {
      return false;
    }

    switch (req.getMethod()) {
      case "DELETE": {
        this.doDelete(req, res);
        break;
      }
      case "GET": {
        this.doGet(req, res);
        break;
      }
      case "POST": {
        this.doPost(req, res);
        break;
      }
      default: {
        return false;
      }
    }

    return true;
  }

  private void onMethodInvocation(Event event) {
    Value requestParam = event.popParameter("req");
    if (requestParam == null) {
      return;
    }

    event.popParameter("resp");

    HttpServletRequest request = requestParam.get();
    event.setHttpServerRequest(request.getMethod(), request.getRequestURI(), request.getProtocol());

    Map<String, String[]> params = request.getParameterMap();
    for (Map.Entry<String, String[]> entry : params.entrySet()) {
      event.addMessageParam(entry.getKey(), String.join(" ", entry.getValue()));
    }

    event.setParameters(null);

    recorder.add(event);
  }

  private void onMethodReturn(Event event) {
    // Value responseParam = event.getParameter("resp");
    // if (responseParam == null) {
    //   return null;
    // }

    // HttpServletResponse response = responseParam.get();
    // HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response);
    // event.setHttpServerResponse(responseWrapper.getStatus());
    recorder.add(event);
  }

  @Override
  public Boolean processEvent(Event event, ThreadLock lock) {
    if (this.handleRequest(event)) {
      return false;
    }

    if (event.event.equals("call")) {
      this.onMethodInvocation(event);
    } else if (event.event.equals("return")) {
      this.onMethodReturn(event);
    }

    return true;
  }
}
