package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.HttpServerRequest;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.Recorder;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ServletFilterReceiver implements IEventProcessor {
  public static final String RecordRoute = "/_appmap/record";

  @Override
  public Boolean onEnter(Event event) {
    Value   reqValue = event.getParameter(0);
    Value   resValue = event.getParameter(1);
    Value chainValue = event.getParameter(2);

    if (reqValue == null || resValue == null || chainValue == null) {
      System.err.println("failed to handle servlet filter chain");
      return true;
    }

    if ( !(reqValue.get() instanceof HttpServletRequest) ) {
      System.err.println("Servlet request value " + reqValue.get().getClass().getName() + " is not an HttpServletRequest");
      return true;
    }

    HttpServletRequest  req = reqValue.get();
    HttpServletResponse res = resValue.get();
    FilterChain chain = chainValue.get();

    if (!req.getRequestURI().equals(ServletFilterReceiver.RecordRoute)) {
      return true;
    }

    try {
      // Allow the next filter in the chain to be handled by releasing the lock on this thread
      ThreadProcessorStack.current().setLock(false);

      chain.doFilter(req, res);
    } catch (Exception e) {
      System.err.printf("failed to override servlet filter: %s\n", e.getMessage());
    }

    return false;
  }

  @Override
  public void onExit(Event event) { }
}
