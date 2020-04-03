package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.CallbackOn;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Hooks to capture @{code http_server_request} and @{code http_server_response} data.
 */
@Unique("http_server_request")
public class HttpServerRequest {
  private static final Recorder recorder = Recorder.getInstance();

  private static void recordHttpServerRequest(Event event, HttpServletRequest req) {
    if (req.getRequestURI().endsWith(ToggleRecord.RecordRoute)) {
      return;
    }

    event.setHttpServerRequest(req.getMethod(), req.getRequestURI(), req.getProtocol());
    event.setParameters(null);
    recorder.add(event);
  }

  private static void recordHttpServerResponse(Event event, HttpServletResponse res) {
    event.setHttpServerResponse(res.getStatus(), res.getContentType());
    event.setParameters(null);
    recorder.add(event);
  }

  @HookClass(value = "javax.servlet.Filter")
  public static void doFilter(Event event,
                              Filter self,
                              ServletRequest req,
                              ServletResponse res,
                              FilterChain chain) {
    recordHttpServerRequest(event, (HttpServletRequest) req);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass(value = "javax.servlet.Filter")
  public static void doFilter(Event event,
                              Filter self,
                              Object returnValue,
                              ServletRequest req,
                              ServletResponse res,
                              FilterChain chain) {
    recordHttpServerResponse(event, (HttpServletResponse) res);
  }

  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @HookClass(value = "javax.servlet.Filter")
  public static void doFilter(Event event,
                              Filter self,
                              Throwable exception,
                              ServletRequest req,
                              ServletResponse res,
                              FilterChain chain) {
    event.setEvent("exception");
    event.setReturnValue(exception);
    recorder.add(event);
  }

  @HookClass(value = "javax.servlet.http.HttpServlet")
  public static void service( Event event,
                              HttpServlet self,
                              HttpServletRequest req,
                              HttpServletResponse res) {
    recordHttpServerRequest(event, req);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass(value = "javax.servlet.http.HttpServlet")
  public static void service( Event event,
                              HttpServlet self,
                              Object returnValue,
                              HttpServletRequest req,
                              HttpServletResponse res) {
    recordHttpServerResponse(event, res);
  }

  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @HookClass(value = "javax.servlet.http.HttpServlet")
  public static void service( Event event,
                              HttpServlet self,
                              Throwable exception,
                              HttpServletRequest req,
                              HttpServletResponse res) {
    event.setEvent("exception");
    event.setReturnValue(exception);
    recorder.add(event);
  }
}
