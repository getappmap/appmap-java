package com.appland.appmap.web;

import com.appland.appmap.process.EventDispatcher;
import com.appland.appmap.record.RuntimeRecorder;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "HelloServlet", urlPatterns = {"/_appmap"}, loadOnStartup = 1)
class AppMapServlet extends HttpServlet {
  private static final String recordRoute = "/_appmap/record";
  private static final RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();

  private static Boolean requestValidated(HttpServletRequest req, HttpServletResponse resp) {
    if (req.getRequestURI().equals(recordRoute) == false) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return false;
    }

    return true;
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) 
      throws ServletException, IOException {
    if (AppMapServlet.requestValidated(req, resp) == false) {
      return;
    }

    if (EventDispatcher.isEnabled() == false) {
      resp.setStatus(HttpServletResponse.SC_CONFLICT);
      return;
    }

    EventDispatcher.setEnabled(false);
    AppMapServlet.runtimeRecorder.dumpJson();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
      throws ServletException, IOException {
    if (AppMapServlet.requestValidated(req, resp) == false) {
      return;
    }

    resp.setStatus(HttpServletResponse.SC_OK);

    String responseJson = String.format("{\"enabled\":%b}", EventDispatcher.isEnabled());
    PrintWriter  writer = resp.getWriter();

    writer.write(responseJson);
    writer.flush();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
      throws ServletException, IOException {
    if (AppMapServlet.requestValidated(req, resp) == false) {
      return;
    }

    if (EventDispatcher.isEnabled()) {
      resp.setStatus(HttpServletResponse.SC_CONFLICT);
      return;
    }

    EventDispatcher.setEnabled(true);
  }
}