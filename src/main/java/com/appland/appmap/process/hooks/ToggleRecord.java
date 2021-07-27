package com.appland.appmap.process.hooks;

import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.process.conditions.RecordCondition;
import com.appland.appmap.record.*;
import com.appland.appmap.reflect.FilterChain;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.transform.annotations.*;
import com.appland.appmap.util.Logger;

import java.io.IOException;
import java.io.PrintWriter;

import static com.appland.appmap.util.StringUtil.*;

interface HandlerFunction {
  void call(HttpServletRequest req, HttpServletResponse res) throws IOException;
}

/**
 * Hooks to toggle event recording. This could be either via HTTP or by entering a unit test method.
 */
public class ToggleRecord {
  private static boolean debug = Properties.DebugHttp;
  private static final Recorder recorder = Recorder.getInstance();
  public static final String RecordRoute = "/_appmap/record";
  public static final String CheckpointRoute = "/_appmap/record/checkpoint";

  private static void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
    if (debug) {
      Logger.println("ToggleRecord.doDelete");
    }

    if (!recorder.hasActiveSession()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Recording recording = recorder.stop();
    res.setContentType("application/json");
    res.setContentLength(recording.size());

    recording.readFully(true, res.getWriter());
  }

  private static void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    if (debug) {
      Logger.println("ToggleRecord.doGet");
    }

    String responseJson = String.format("{\"enabled\":%b}", recorder.hasActiveSession());
    res.setContentType("application/json");
    res.setContentLength(responseJson.length());
    res.setStatus(HttpServletResponse.SC_OK);

    PrintWriter writer = res.getWriter();
    writer.write(responseJson);
    writer.flush();
  }

  private static void doPost(HttpServletRequest req, HttpServletResponse res) {
    if (debug) {
      Logger.println("ToggleRecord.doPost");
    }

    if (recorder.hasActiveSession()) {
      res.setStatus(HttpServletResponse.SC_CONFLICT);
      return;
    }

    RecordingSession.Metadata metadata = new RecordingSession.Metadata();
    metadata.recorderName = "remote_recording";
    recorder.start(metadata);
  }

  private static void doCheckpoint(HttpServletRequest req, HttpServletResponse res) {
    if (debug) {
      Logger.println("ToggleRecord.doCheckpoint");
    }

    if (!recorder.hasActiveSession()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Recording recording = recorder.checkpoint();
    res.setContentType("application/json");
    res.setContentLength(recording.size());

    try {
      recording.readFully(true, res.getWriter());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void handleRecordRequest(HttpServletRequest req, HttpServletResponse res, HandlerFunction fn) throws ExitEarly {
    if (debug) {
      Logger.printf("ToggleRecord.service - handling appmap request for %s\n", req.getRequestURI());
    }

    try {
      fn.call(req, res);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (debug) {
      Logger.println("ToggleRecord.service - successfully handled appmap request, exiting early");
    }

    throw new ExitEarly();
  }

  private static void service(Object[] args) throws ExitEarly {
    if (args.length != 2) {
      return;
    }

    final HttpServletRequest req = new HttpServletRequest(args[0]);
    final HttpServletResponse res = new HttpServletResponse(args[1]);

    if (req.getRequestURI().endsWith(CheckpointRoute)) {
      if (req.getMethod().equals("GET")) {
        handleRecordRequest(req, res, ToggleRecord::doCheckpoint);
      }
    } else if (req.getRequestURI().endsWith(RecordRoute)) {
      if (req.getMethod().equals("GET")) {
        handleRecordRequest(req, res, ToggleRecord::doGet);
      } else if (req.getMethod().equals("POST")) {
        handleRecordRequest(req, res, ToggleRecord::doPost);
      } else if (req.getMethod().equals("DELETE")) {
        handleRecordRequest(req, res, ToggleRecord::doDelete);
      }
    }
  }

  private static void skipFilterChain(Object[] args) throws ExitEarly {
    if (args.length != 3) {
      if (debug) {
        Logger.println("ToggleRecord.skipFilterChain - invalid arg length");
      }
      return;
    }

    final HttpServletRequest req = new HttpServletRequest(args[0]);
    if (!req.getRequestURI().endsWith(RecordRoute)) {
      return;
    }

    if (debug) {
      Logger.println("ToggleRecord.skipFilterChain - skipping filter chain");
    }

    final FilterChain chain = new FilterChain(args[2]);
    chain.doFilter(args[0], args[1]);

    if (debug) {
      Logger.println("ToggleRecord.skipFilterChain - successfully skipped, exiting early");
    }

    throw new ExitEarly();
  }

  @ExcludeReceiver
  @ArgumentArray
  @HookClass("javax.servlet.http.HttpServlet")
  public static void service(Event event, Object[] args) throws ExitEarly {
    service(args);
  }

  @ExcludeReceiver
  @ArgumentArray
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service")
  public static void serviceJakarta(Event event, Object[] args) throws ExitEarly {
    service(args);
  }

  @ContinueHooking
  @ExcludeReceiver
  @ArgumentArray
  @HookClass("javax.servlet.Filter")
  public static void doFilter(Event event, Object[] args) throws ExitEarly {
    skipFilterChain(args);
  }

  @ContinueHooking
  @ExcludeReceiver
  @ArgumentArray
  @HookClass(value = "jakarta.servlet.Filter", method = "doFilter")
  public static void doFilterJakarta(Event event, Object[] args) throws ExitEarly {
    skipFilterChain(args);
  }

  private static RecordingSession.Metadata getMetadata(Event event) {
    // TODO: Obtain this info in the constructor
    boolean junit = false;
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    for (int i = 0; !junit && i < stack.length; i++) {
      if (stack[i].getClassName().startsWith("org.junit")) {
        junit = true;
      }
    }
    RecordingSession.Metadata metadata = new RecordingSession.Metadata();
    if (junit) {
      metadata.recorderName = "toggle_record_receiver";
      metadata.framework = "junit";
    } else {
      metadata.recorderName = canonicalName(event.definedClass, event.isStatic, event.methodId);
    }
    return metadata;
  }

  private static void startRecording(Event event) {
    Logger.printf("Recording started for %s\n", canonicalName(event));
    try {
      RecordingSession.Metadata metadata = getMetadata(event);
      final String feature = identifierToSentence(event.methodId);
      final String featureGroup = identifierToSentence(event.definedClass);
      metadata.scenarioName = String.format(
          "%s %s",
          featureGroup,
          decapitalize(feature));
      metadata.recordedClassName = event.definedClass;
      metadata.recordedMethodName = event.methodId;
      recorder.start(metadata);
    } catch (ActiveSessionException e) {
      Logger.printf("%s\n", e.getMessage());
    }
  }

  private static void stopRecording(Event event) {
    Logger.printf("Recording stopped for %s\n", canonicalName(event));
    String filePath = String.join("_", event.definedClass, event.methodId)
        .replaceAll("[^a-zA-Z0-9-_]", "_");
    filePath += ".appmap.json";
    Recording recording = recorder.stop();
    recording.moveTo(filePath);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void junit(Event event, Object[] args) {
    startRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void junit(Event event, Object returnValue, Object[] args) {
    stopRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void junit(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    stopRecording(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.jupiter.api.Test")
  public static void junitJupiter(Event event, Object[] args) {
    startRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @ExcludeReceiver
  @HookAnnotated("org.junit.jupiter.api.Test")
  public static void junitJupiter(Event event, Object returnValue, Object[] args) {
    stopRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @ExcludeReceiver
  @HookAnnotated("org.junit.jupiter.api.Test")
  public static void junitJupiter(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    stopRecording(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.testng.annotations.Test")
  public static void testng(Event event, Object[] args) {
    startRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @ExcludeReceiver
  @HookAnnotated("org.testng.annotations.Test")
  public static void testnt(Event event, Object returnValue, Object[] args) {
    stopRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @ExcludeReceiver
  @HookAnnotated("org.testng.annotations.Test")
  public static void testng(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    stopRecording(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookCondition(RecordCondition.class)
  public static void record(Event event, Object[] args) {
    startRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @ExcludeReceiver
  @HookCondition(RecordCondition.class)
  public static void record(Event event, Object returnValue, Object[] args) {
    stopRecording(event);
  }

  @ArgumentArray
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @ExcludeReceiver
  @HookCondition(RecordCondition.class)
  public static void record(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    stopRecording(event);
  }
}
