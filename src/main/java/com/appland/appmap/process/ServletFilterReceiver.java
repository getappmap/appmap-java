package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.HttpServerRequest;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.process.EventDispatcher;
import com.appland.appmap.record.RuntimeRecorder;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ServletFilterReceiver implements IEventProcessor {
  private static final String recordRoute = "/_appmap/record";

  @Override
  public int processEvent(Event event) {
    if (event.event.equals("call")) {
      Value   reqValue = event.getParameter(0);
      Value   resValue = event.getParameter(1);
      Value chainValue = event.getParameter(2);

      if (reqValue == null || resValue == null || chainValue == null) {
        System.err.println("failed to handle servlet filter chain");
        return EventDispatcher.EVENT_DISCARD;
      }

      HttpServletRequest  req = reqValue.get();
      HttpServletResponse res = resValue.get();
      FilterChain chain = chainValue.get();

      if (req.getRequestURI().equals(ServletFilterReceiver.recordRoute)) {
        EventDispatcher.invoke(() -> {
          try {
            chain.doFilter(req, res);
          } catch (Throwable e) {
            System.err.printf("failed to override servlet filter: %s\n", e.getMessage());
          }
        });
        return EventDispatcher.EVENT_DISCARD | EventDispatcher.EVENT_EXIT_EARLY;
      }
    }

    return EventDispatcher.EVENT_DISCARD;
  }
}
