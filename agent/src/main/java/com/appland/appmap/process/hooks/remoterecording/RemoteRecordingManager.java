package com.appland.appmap.process.hooks.remoterecording;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.reflect.HttpServletResponse;

interface RemoteRecordingRequest {
  String getRequestURI();
  String getMethod();
  void setStatus(int status);
  void writeJson(String responseJson) throws IOException;
  void writeRecording(Recording recording) throws IOException;
}

public class RemoteRecordingManager {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private static final Recorder recorder = Recorder.getInstance();
  public static final String RecordRoute = "/_appmap/record";
  public static final String CheckpointRoute = "/_appmap/record/checkpoint";

  private static void doDelete(RemoteRecordingRequest req) throws IOException {
    logger.trace("req: {}", req);

    if (!recorder.hasActiveSession()) {
      req.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Recording recording = recorder.stop();
    req.writeRecording(recording);
    req.setStatus(HttpServletResponse.SC_OK);
  }

  private static void doGet(RemoteRecordingRequest req) throws IOException {
    logger.trace("req: {}", req);

    String responseJson = String.format(
      "{\"enabled\":%b}",
      recorder.hasActiveSession()
    );
    req.writeJson(responseJson);
    req.setStatus(HttpServletResponse.SC_OK);
  }

  private static void doPost(RemoteRecordingRequest req) {
    logger.trace("req: {}", req);

    if (recorder.hasActiveSession()) {
      logger.trace("recording in progress");
      req.setStatus(HttpServletResponse.SC_CONFLICT);
      return;
    }

    Recorder.Metadata metadata = new Recorder.Metadata("remote_recording", "remote");
    metadata.scenarioName = String.format("Remote Recording - %s",
        Recording.RECORDING_TIME_FORMATTER.format(OffsetDateTime.now()));
    recorder.start(metadata);
    req.setStatus(HttpServletResponse.SC_OK);
    logger.trace("recording started");
  }

  private static void doCheckpoint(RemoteRecordingRequest req)
    throws IOException {
    logger.trace("req: {}");

    if (!recorder.hasActiveSession()) {
      req.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Recording recording = recorder.checkpoint();
    req.writeRecording(recording);
    req.setStatus(HttpServletResponse.SC_OK);
  }

  interface HandlerFunction {
    void call(RemoteRecordingRequest req) throws IOException;
  }

  private static void handleRecordRequest(
    RemoteRecordingRequest req,
    HandlerFunction fn
  )
    throws ExitEarly {
    try {
      fn.call(req);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean service(RemoteRecordingRequest req) {
    boolean handled = false;
    String method = req.getMethod();
    String requestURI = req.getRequestURI();
    logger.debug("req: {}", () -> String.format("%s %s", method, requestURI));
    if (requestURI.endsWith(CheckpointRoute)) {
      if (method.equals("GET")) {
        handleRecordRequest(req, RemoteRecordingManager::doCheckpoint);
        handled = true;
      }
    } else if (requestURI.endsWith(RecordRoute)) {
      if (method.equals("GET")) {
        handleRecordRequest(req, RemoteRecordingManager::doGet);
        handled = true;
      } else if (method.equals("POST")) {
        handleRecordRequest(req, RemoteRecordingManager::doPost);
        handled = true;
      } else if (method.equals("DELETE")) {
        handleRecordRequest(req, RemoteRecordingManager::doDelete);
        handled = true;
      }
    }

    logger.debug("handled appmap request? {}", handled);

    return handled;
  }
}
