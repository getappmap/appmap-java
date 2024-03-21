package com.appland.appmap.process.hooks;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.process.hooks.http.HttpServerRequest;
import com.appland.appmap.process.hooks.http.ServletListener;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.record.RecordingSession;
import com.appland.appmap.reflect.HttpServletRequest;

public class RequestRecording {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  public static void start(HttpServletRequest req) {
    String uri = req.getRequestURI();
    logger.trace("request init, {} {}", req.getMethod(), uri);

    if (uri.equals("/_appmap/record")) {
      return;
    }

    Recorder.Metadata metadata = new Recorder.Metadata("request_recording", "requests");

    RecordingSession recordingSession = new RecordingSession(metadata);

    try {
      Recorder.INSTANCE.setThreadSession(recordingSession);
      req.setAttribute(ServletListener.RECORDING_ATTRIBUTE, recordingSession);
    } catch (ActiveSessionException e) {
      ServletListener.logger.warn(e);
    }
  }

  public static void stop(HttpServletRequest req) {
    String uri = req.getRequestURI();
    logger.trace("request destroy, {} {}", req.getMethod(), uri);

    if (uri.equals("/_appmap/record")) {
      return;
    }

    RecordingSession recordingSession = (RecordingSession) req.getAttribute(ServletListener.RECORDING_ATTRIBUTE);
    if (recordingSession == null) {
      Consumer<String> logfn = Properties.RecordingRequests ? logger::debug : logger::warn;
      logfn.accept("No recording found for this request, no AppMap will be created");
      return;
    }

    Integer statusAttr = (Integer) req.getAttribute(HttpServerRequest.STATUS_ATTRIBUTE);
    String status = statusAttr != null ? statusAttr.toString() : "UNKNOWN";

    Instant startTime = recordingSession.getStartTime();
    LocalDateTime localStart = LocalDateTime.ofInstant(startTime, ZoneId.systemDefault());

    String appMapName = String.format("%s %s (%s) - %s", req.getMethod(), req.getRequestURI(), status,
        localStart.format(Recording.RECORDING_TIME_FORMATTER));
    recordingSession.getMetadata().scenarioName = appMapName;

    Recording recording = Recorder.INSTANCE.stopThread();
    String filename = String.format("%.3f_%s", startTime.toEpochMilli() / 1000.0, req.getRequestURI());
    filename = Recorder.sanitizeFilename(filename) + ".appmap.json";
    recording.moveTo(filename);
  }

  /**
   * abort stops the current thread's recording and throws it away.
   */
  public static void abort() {
    Recorder.INSTANCE.stopThread();
  }

}
