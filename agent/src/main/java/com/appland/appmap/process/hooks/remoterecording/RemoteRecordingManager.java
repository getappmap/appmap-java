package com.appland.appmap.process.hooks.remoterecording;

import java.io.IOException;

import com.appland.appmap.config.Properties;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.util.Logger;

interface RemoteRecordingRequest {
  String getRequestURI();
  String getMethod();
  void setStatus(int status);
  void writeJson(String responseJson) throws IOException;
  void writeRecording(Recording recording) throws IOException;
}

public class RemoteRecordingManager {

  private static final boolean debug = Properties.DebugHttp;
  private static final Recorder recorder = Recorder.getInstance();
  public static final String RecordRoute = "/_appmap/record";
  public static final String CheckpointRoute = "/_appmap/record/checkpoint";

  private static void doDelete(RemoteRecordingRequest req) throws IOException {
    if (debug) {
      Logger.println("RemoteRecordingManager.doDelete");
    }

    if (!recorder.hasActiveSession()) {
      req.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Recording recording = recorder.stop();
    req.writeRecording(recording);
    req.setStatus(HttpServletResponse.SC_OK);
  }

  private static void doGet(RemoteRecordingRequest req) throws IOException {
    if (debug) {
      Logger.println("RemoteRecordingManager.doGet");
    }

    String responseJson = String.format(
      "{\"enabled\":%b}",
      recorder.hasActiveSession()
    );
    req.writeJson(responseJson);
    req.setStatus(HttpServletResponse.SC_OK);
  }

  private static void doPost(RemoteRecordingRequest req) {
    if (debug) {
      Logger.println("RemoteRecordingManager.doPost");
    }

    if (recorder.hasActiveSession()) {
      req.setStatus(HttpServletResponse.SC_CONFLICT);
      return;
    }

    Recorder.Metadata metadata = new Recorder.Metadata("remote_recording", "remote");
    recorder.start(metadata);
    req.setStatus(HttpServletResponse.SC_OK);
  }

  private static void doCheckpoint(RemoteRecordingRequest req)
    throws IOException {
    if (debug) {
      Logger.println("RemoteRecordingManager.doCheckpoint");
    }

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
    if (debug) {
      Logger.printf(
        "RemoteRecordingManager.service - handling appmap request for %s\n",
        req
      );
    }
    Logger.println("service, ret.getRequestUri(): " + req.getRequestURI());
    if (req.getRequestURI().endsWith(CheckpointRoute)) {
      if (req.getMethod().equals("GET")) {
        handleRecordRequest(req, RemoteRecordingManager::doCheckpoint);
        handled = true;
      }
    } else if (req.getRequestURI().endsWith(RecordRoute)) {
      if (req.getMethod().equals("GET")) {
        handleRecordRequest(req, RemoteRecordingManager::doGet);
        handled = true;
      } else if (req.getMethod().equals("POST")) {
        handleRecordRequest(req, RemoteRecordingManager::doPost);
        handled = true;
      } else if (req.getMethod().equals("DELETE")) {
        handleRecordRequest(req, RemoteRecordingManager::doDelete);
        handled = true;
      }
    }

    if (debug) {
      Logger.println(
        "RemoteRecordingManager.service - handled appmap request? " + handled
      );
    }

    return handled;
  }
}
