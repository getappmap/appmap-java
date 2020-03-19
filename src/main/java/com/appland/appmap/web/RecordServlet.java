package com.appland.appmap.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.annotation.WebServlet;

@WebServlet(name = "RecordServlet", urlPatterns = {"/_appmap/record"}, loadOnStartup = 1) 
public class RecordServlet extends HttpServlet {
  private static final Recorder recorder = Recorder.getInstance();

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
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

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
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

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) {
    IRecordingSession.Metadata metadata = new IRecordingSession.Metadata();
    metadata.recorderName = "remote_recording";
    try {
      recorder.start(metadata);
    } catch (ActiveSessionException e) {
      res.setStatus(HttpServletResponse.SC_CONFLICT);
    }
  }
}