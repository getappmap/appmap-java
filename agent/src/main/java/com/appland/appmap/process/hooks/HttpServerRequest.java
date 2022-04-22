package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.remoterecording.RemoteRecordingManager;
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
    if (req.getRequestURI().endsWith(RemoteRecordingManager.RecordRoute)) {
      return;
    }

    recordHttpServerRequest(event,
      req.getMethod(), req.getRequestURI(), req.getProtocol(), 
      req.getHeaders(),
      req.getParameterMap());
  }

  public static void recordHttpServerRequest(Event event, 
    String method, String uri, String protocol, 
    Map<String, String> headers,
    Map<String, String[]> params)  {
    event.setHttpServerRequest(method, uri, protocol, headers);
    event.setParameters(null);
    
    if (params != null) {
      for (Map.Entry<String, String[]> param : params.entrySet()) {
        final String[] values = param.getValue();
        event.addMessageParam(param.getKey(), values.length > 0 ? values[0] : "");
      }
    }
    recorder.add(event);
  }

  private static void recordHttpServerResponse(Event event, HttpServletResponse res) {
    recordHttpServerResponse(event, res.getStatus(), res.getHeaders());
  }

  public static void recordHttpServerResponse(Event event, int status, Map<String,String> headers) {
    event.setHttpServerResponse(status, headers);
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
  @HookClass(value = "javax.servlet.Filter", methodEvent = MethodEvent.METHOD_RETURN)

  public static void doFilter(Event event, Object returnValue, Object[] args) {
    if (args.length != 3) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.Filter", method = "doFilter", methodEvent = MethodEvent.METHOD_RETURN)
  public static void doFilterJakarta(Event event, Object returnValue, Object[] args) {
    if (args.length != 3) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "javax.servlet.Filter", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void doFilter(Event event, Exception exception, Object[] args) {
    if (args.length != 3) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.Filter", method = "doFilter", methodEvent = MethodEvent.METHOD_EXCEPTION)
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
  @HookClass(value = "javax.servlet.http.HttpServlet", methodEvent = MethodEvent.METHOD_RETURN)
  public static void service(Event event, Object returnValue, Object[] args) {
    if (args.length != 2) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service", methodEvent = MethodEvent.METHOD_RETURN)
  public static void serviceJakarta(Event event, Object returnValue, Object[] args) {
    if (args.length != 2) {
      return;
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "javax.servlet.http.HttpServlet", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void service( Event event, Exception exception, Object[] args) {
    if (args.length != 2) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void serviceJakarta( Event event, Exception exception, Object[] args) {
    if (args.length != 2) {
      return;
    }

    event.setException(exception);
    recorder.add(event);
  }
}
