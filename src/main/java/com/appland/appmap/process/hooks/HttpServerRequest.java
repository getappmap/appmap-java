package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.CallbackOn;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;

import java.util.Map;

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

    for (Map.Entry<String, String[]> param : req.getParameterMap().entrySet()) {
      final String[] values = param.getValue();
      event.addMessageParam(param.getKey(), values.length > 0 ? values[0] : "");
    }

    recorder.add(event);
  }

  private static void recordHttpServerResponse(Event event, HttpServletResponse res) {
    event.setHttpServerResponse(res.getStatus(), res.getContentType());
    event.setParameters(null);
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass("javax.servlet.Filter")
  public static void doFilter(Event event, Object[] args) {
                              // Filter self,
                              // ServletRequest req,
                              // ServletResponse res,
                              // FilterChain chain) {
    if (args.length != 3) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    recordHttpServerRequest(event, req);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("javax.servlet.Filter")
  public static void doFilter(Event event, Object returnValue, Object[] args) {
                              // Filter self,
                              // Object returnValue,
                              // ServletRequest req,
                              // ServletResponse res,
                              // FilterChain chain) {
    if (args.length != 3) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @HookClass("javax.servlet.Filter")
  public static void doFilter(Event event, Exception exception, Object[] args) {
                              // Filter self,
                              // Exception exception,
                              // ServletRequest req,
                              // ServletResponse res,
                              // FilterChain chain) {
    if (args.length != 3) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass("javax.servlet.http.HttpServlet")
  public static void service( Event event, Object[] args) {
                              // HttpServlet self,
                              // HttpServletRequest req,
                              // HttpServletResponse res) {
    if (args.length != 2) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    recordHttpServerRequest(event, req);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("javax.servlet.http.HttpServlet")
  public static void service( Event event, Object returnValue, Object[] args) {
                              // HttpServlet self,
                              // Object returnValue,
                              // HttpServletRequest req,
                              // HttpServletResponse res) {
    if (args.length != 2) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @HookClass("javax.servlet.http.HttpServlet")
  public static void service( Event event, Exception exception, Object[] args) {
                              // HttpServlet self,
                              // Exception exception,
                              // HttpServletRequest req,
                              // HttpServletResponse res) {
    if (args.length != 2) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }
}
