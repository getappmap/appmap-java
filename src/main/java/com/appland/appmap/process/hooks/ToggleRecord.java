package com.appland.appmap.process.hooks;

import static com.appland.appmap.util.StringUtil.identifierToSentence;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.*;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Hooks to toggle event recording. This could be either via HTTP or by entering a unit test method.
 */
public class ToggleRecord {
  private static final Recorder recorder = Recorder.getInstance();
  public static final String RecordRoute = "/_appmap/record";

  private static void doDelete(HttpServletRequest req, HttpServletResponse res) {
    try {
      String json = recorder.stop();
      res.setContentType("application/json");
      res.setContentLength(json.length());

      PrintWriter writer = res.getWriter();
      writer.write(json);
      writer.flush();
    } catch (ActiveSessionException e) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (IOException e) {
      System.err.printf("failed to write response: %s\n", e.getMessage());
    }
  }

  private static void doGet(HttpServletRequest req, HttpServletResponse res) {
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

  private static void doPost(HttpServletRequest req, HttpServletResponse res) {
    IRecordingSession.Metadata metadata = new IRecordingSession.Metadata();
    metadata.recorderName = "remote_recording";
    try {
      recorder.start(metadata);
    } catch (ActiveSessionException e) {
      res.setStatus(HttpServletResponse.SC_CONFLICT);
    }
  }

  @ExcludeReceiver
  @HookClass("javax.servlet.http.HttpServlet")
  public static void service(Event event, HttpServletRequest req, HttpServletResponse res)
      throws ExitEarly {
    if (!req.getRequestURI().endsWith(RecordRoute)) {
      return;
    }

    if (req.getMethod().equals("GET")) {
      doGet(req, res);
    } else if (req.getMethod().equals("POST")) {
      doPost(req, res);
    } else if (req.getMethod().equals("DELETE")) {
      doDelete(req, res);
    }

    throw new ExitEarly();
  }

  @ContinueHooking
  @ExcludeReceiver
  @HookClass("javax.servlet.Filter")
  public static void doFilter(Event event,
                              ServletRequest req,
                              ServletResponse res,
                              FilterChain chain)
                              throws IOException, ServletException, ExitEarly {
    if (!(req instanceof HttpServletRequest)) {
      return;
    }

    if (!((HttpServletRequest) req).getRequestURI().endsWith(RecordRoute)) {
      return;
    }

    chain.doFilter(req, res);

    throw new ExitEarly();
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void startTest(Event event, Object[] args) {
    try {
      final String fileName = String.join("_", event.definedClass, event.methodId)
          .replaceAll("[^a-zA-Z0-9-_]", "_");

      IRecordingSession.Metadata metadata = new IRecordingSession.Metadata();

      // TODO: Obtain this info in the constructor
      boolean junit = false;
      StackTraceElement[] stack = Thread.currentThread().getStackTrace();
      for (int i = 0; !junit && i < stack.length; i++) {
        if (stack[i].getClassName().startsWith("org.junit")) {
          junit = true;
        }
      }

      metadata.feature = identifierToSentence(event.methodId);
      metadata.featureGroup = identifierToSentence(event.definedClass);
      metadata.recordedClassName = event.definedClass;
      metadata.recordedMethodName = event.methodId;
      if (junit) {
        metadata.recorderName = "toggle_record_receiver";
        metadata.framework = "junit";
      }

      recorder.start(fileName, metadata);
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: %s\n", e.getMessage());
    }
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void stopTest(Event event, Object returnValue, Object[] args) {
    try {
      recorder.stop();
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: %s\n", e.getMessage());
    }
  }
}
