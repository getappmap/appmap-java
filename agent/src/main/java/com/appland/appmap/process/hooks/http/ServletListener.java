package com.appland.appmap.process.hooks.http;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.record.RecordingSession;
import com.appland.appmap.reflect.DynamicReflectiveType;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.ServletRequestEvent;

public class ServletListener implements InvocationHandler {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static final DateTimeFormatter RECORDING_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  private static final String PACKAGE_NAME = MethodHandles.lookup().lookupClass().getPackage().getName();
  private static final String RECORDING_ATTRIBUTE = PACKAGE_NAME + ".recording";

  private ServletListener() {
  }

  public static Object build() {
    return DynamicReflectiveType.build(new ServletListener(), "javax.servlet.ServletRequestListener",
        "jakarta.servlet.ServletRequestListener");
  }

  private static void requestInitialized(ServletRequestEvent evt) {
    HttpServletRequest req = evt.getServletRequest();
    ServletContext ctx = req.getServletContext();
    String ctxName = ctx.getServletContextName();
    String uri = req.getRequestURI();
    logger.trace("{}: request init, {} {}", ctxName, req.getMethod(), uri);

    if (uri.equals("/_appmap/record")) {
      return;
    }

    Recorder.Metadata metadata = new Recorder.Metadata("request_recording", "request");

    RecordingSession recordingSession = new RecordingSession(metadata);

    try {
      Recorder.getInstance().setThreadSession(recordingSession);
      req.setAttribute(RECORDING_ATTRIBUTE, recordingSession);
    } catch (ActiveSessionException e) {
      logger.warn(e);
    }
  }

  private static void requestDestroyed(ServletRequestEvent evt) {
    HttpServletRequest req = evt.getServletRequest();
    ServletContext ctx = req.getServletContext();
    String ctxName = ctx.getServletContextName();
    String uri = req.getRequestURI();

    logger.trace("{}: request destroy, {} {}", ctxName, req.getMethod(), uri);

    if (uri.equals("/_appmap/record")) {
      return;
    }

    RecordingSession recordingSession = (RecordingSession) req.getAttribute(RECORDING_ATTRIBUTE);
    if (recordingSession == null) {
      Consumer<String> logfn = Properties.RecordRequests ? logger::debug : logger::warn;
      logfn.accept("No recording found for this request, no AppMap will be created");
      return;
    }

    Integer statusAttr = (Integer) req.getAttribute(HttpServerRequest.STATUS_ATTRIBUTE);
    String status = statusAttr != null ? statusAttr.toString() : "UNKNOWN";

    Instant startTime = recordingSession.getStartTime();
    LocalDateTime localStart = LocalDateTime.ofInstant(startTime, ZoneId.systemDefault());

    String appMapName = String.format("%s %s (%s) - %s", req.getMethod(), req.getRequestURI(), status,
        localStart.format(RECORDING_TIME_FORMATTER));
    recordingSession.getMetadata().scenarioName = appMapName;

    Recording recording = Recorder.getInstance().stopThread();
    String filename = String.format("%.3f_%s", startTime.toEpochMilli() / 1000.0, req.getRequestURI());
    filename = Recorder.sanitizeFilename(filename) + ".appmap.json";
    recording.moveTo(filename);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().equals("requestInitialized")) {
      requestInitialized(new ServletRequestEvent(args[0]));
    } else if (method.getName().equals("requestDestroyed")) {
      requestDestroyed(new ServletRequestEvent(args[0]));
    }

    return null;
  }
}