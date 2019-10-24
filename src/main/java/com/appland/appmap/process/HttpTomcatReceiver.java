package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.process.EventDispatcher;
import com.appland.appmap.record.RuntimeRecorder;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class HttpTomcatReceiver implements IEventProcessor {
  private static final String recordRoute = "/_appmap/record";
  private static final RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();

  private void doDelete(HttpServletRequest req, HttpServletResponse res) {
    if (EventDispatcher.isEnabled() == false) {
      res.setStatus(HttpServletResponse.SC_CONFLICT);
      return;
    }

    String json = runtimeRecorder.dumpJson();
    res.setContentType("application/json");
    res.setContentLength(json.length());

    try {
      PrintWriter writer = res.getWriter();
      writer.write(json);
      writer.flush();
    } catch (IOException e) {
      System.err.printf("failed to write response: %s\n", e.getMessage());
    }

    EventDispatcher.setEnabled(false);
  }

  private void doGet(HttpServletRequest req, HttpServletResponse res) {
    res.setStatus(HttpServletResponse.SC_OK);

    String responseJson = String.format("{\"enabled\":%b}", EventDispatcher.isEnabled());
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
    if (EventDispatcher.isEnabled()) {
      res.setStatus(HttpServletResponse.SC_CONFLICT);
      return;
    }

    EventDispatcher.setEnabled(true);
  }

  private Boolean handleRequest(Event event) {
    if (event.event.equals("call") == false) {
      return false;
    }

    Value requestValue = event.getParameter("req");
    Value responseValue = event.getParameter("resp");
    if (requestValue == null || responseValue == null) {
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

  private void injectHtml(Event event) {
    if (event.event.equals("return") == false) {
      return;
    }

    Value responseValue = event.getParameter("resp");
    if (responseValue == null) {
      return;
    }

    HttpServletResponse res = responseValue.get();
    if (res.getContentType().startsWith("text/html;") == false) {
      return;
    }

    // Read the current response
    //   -> we may need to proxy access to the buffer to do this... all "writes" would be written
    //      to our proxy buffer, parsed by us, then we'd write everything back into the real buffer
    //      once we've finished everything we need to do. AFAIK there's no reliable way of
    //      recovering bytes written to a writer, nonetheless re-ordering them.
    // Parse the HTML
    // Inject HTML/CSS/JS
  }

  private int onMethodInvocation(Event event) {
    Value requestParam = event.popParameter("req");
    if (requestParam == null) {
      return EventDispatcher.EVENT_DISCARD;
    }
    event.popParameter("resp");

    HttpServletRequest request = requestParam.get();
    event.setHttpServerRequest(request.getMethod(), request.getRequestURI(), request.getProtocol());

    Map<String, String[]> params = request.getParameterMap();
    for (Map.Entry<String, String[]> entry : params.entrySet()) {
      event.addMessageParam(entry.getKey(), String.join(" ", entry.getValue()));
    }

    event.setParameters(null);

    return EventDispatcher.EVENT_RECORD;
  }

  private int onMethodReturn(Event event) {
    // Value responseParam = event.getParameter("resp");
    // if (responseParam == null) {
    //   return null;
    // }

    // HttpServletResponse response = responseParam.get();
    // HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response);
    // event.setHttpServerResponse(responseWrapper.getStatus());

    return EventDispatcher.EVENT_RECORD;
  }

  @Override
  public int processEvent(Event event) {
    if (this.handleRequest(event)) {
      return (EventDispatcher.EVENT_DISCARD | EventDispatcher.EVENT_EXIT_EARLY);
    }

    if (event.event.equals("call")) {
      return this.onMethodInvocation(event);
    } else if (event.event.equals("return")) {
      return this.onMethodReturn(event);
    }

    return EventDispatcher.EVENT_DISCARD;
  }
}
