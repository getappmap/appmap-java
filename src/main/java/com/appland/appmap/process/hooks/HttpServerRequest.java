package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.transform.annotations.*;

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

    event.setHttpServerRequest(req.getMethod(), req.getRequestURI(), req.getProtocol(), req.getHeaders());
    event.setParameters(null);
    

    for (Map.Entry<String, String[]> param : req.getParameterMap().entrySet()) {
      final String[] values = param.getValue();
      event.addMessageParam(param.getKey(), values.length > 0 ? values[0] : "");
    }

    recorder.add(event);
  }

  private static void recordHttpServerResponse(Event event, HttpServletResponse res) {
    event.setHttpServerResponse(res.getStatus(), res.getContentType(), res.getHeaders());
    event.setParameters(null);
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass("javax.servlet.Filter")
  public static void doFilter(Event event, Object[] args) {
    if (args.length != 3) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    recordHttpServerRequest(event, req);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.Filter", method = "doFilter")
  public static void doFilterJakarta(Event event, Object[] args) {
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
    if (args.length != 3) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass(value = "jakarta.servlet.Filter", method = "doFilter")
  public static void doFilterJakarta(Event event, Object returnValue, Object[] args) {
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
    if (args.length != 3) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @HookClass(value = "jakarta.servlet.Filter", method = "doFilter")
  public static void doFilterJakarta(Event event, Exception exception, Object[] args) {
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
    if (args.length != 2) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    recordHttpServerRequest(event, req);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service")
  public static void serviceJakarta( Event event, Object[] args) {
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
  public static void service(Event event, Object returnValue, Object[] args) {
    if (args.length != 2) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service")
  public static void serviceJakarta(Event event, Object returnValue, Object[] args) {
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
    if (args.length != 2) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @CallbackOn(MethodEvent.METHOD_EXCEPTION)
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service")
  public static void serviceJakarta( Event event, Exception exception, Object[] args) {
    if (args.length != 2) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }
}
