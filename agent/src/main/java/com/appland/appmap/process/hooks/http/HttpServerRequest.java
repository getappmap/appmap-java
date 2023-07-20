package com.appland.appmap.process.hooks.http;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.remoterecording.RemoteRecordingManager;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;

/**
 * Hooks to capture @{code http_server_request} and @{code http_server_response}
 * data.
 */
@Unique("http_server_request")
public class HttpServerRequest {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static final String PACKAGE_NAME = MethodHandles.lookup().lookupClass().getPackage().getName();
  public static final String STATUS_ATTRIBUTE = PACKAGE_NAME + ".status";

  // With the inclusion of org.springframework:spring-web as a dependency, both
  // BEST_MATCHING_PATTERN_ATTRIBUTE and URI_TEMPLATE_VARIABLES_ATTRIBUTE now
  // need to be generated at runtime. This works around the string-rewriting
  // issue described in https://github.com/johnrengelman/shadow/issues/232 .

  // Needs to match
  // org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
  private static final String BEST_MATCHING_PATTERN_ATTRIBUTE = "org:springframework:web:servlet:HandlerMapping:bestMatchingPattern"
      .replace(':', '.');
  // Needs to match
  // org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
  private static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE = "org:springframework:web:servlet:HandlerMapping:uriTemplateVariables"
      .replace(':', '.');

  private static final String LAST_EVENT_KEY = PACKAGE_NAME + ".lastEvent";
  private static final Recorder recorder = Recorder.getInstance();

  private static void recordHttpServerRequest(Event event, HttpServletRequest req) {
    if (req.getRequestURI().endsWith(RemoteRecordingManager.RecordRoute)) {
      return;
    }

    recordHttpServerRequest(event, req,
        req.getMethod(), req.getRequestURI(), req.getProtocol(),
        req.getHeaders(),
        req.getParameterMap());
  }

  private static void recordHttpServerRequest(Event event, HttpServletRequest req,
      String method, String uri, String protocol,
      Map<String, String> headers,
      Map<String, String[]> params) {
    event.setHttpServerRequest(method, uri, protocol, headers);
    event.setParameters(null);

    for (Map.Entry<String, String[]> param : params.entrySet()) {
      final String[] values = param.getValue();
      event.addMessageParam(param.getKey(), values.length > 0 ? values[0] : "");
    }

    // Keep track of this event, it may need to be updated after the request is
    // processed.
    req.setAttribute(LAST_EVENT_KEY, event);

    recorder.add(event);
  }

  public static void recordHttpServerRequest(Event event,
      String method, String uri, String protocol,
      Map<String, String> headers) {
    event.setHttpServerRequest(method, uri, protocol, headers);
    event.setParameters(null);

    recorder.add(event);
  }

  private static void recordHttpServerResponse(Event event, HttpServletRequest req, HttpServletResponse res) {
    recordHttpServerResponse(event, req, res.getStatus(), res.getHeaders());
  }

  private static void clearLastEvent(HttpServletRequest req, Event responseEvent) {
    if (req == null) {
      return;
    }

    req.setAttribute(LAST_EVENT_KEY, null);
  }

  public static void recordHttpServerResponse(Event event, HttpServletRequest req, int status,
      Map<String, String> headers) {
    if (req != null) {
      req.setAttribute(STATUS_ATTRIBUTE, Integer.valueOf(status));
    }
    event.setHttpServerResponse(status, headers);
    event.setParameters(null);

    clearLastEvent(req, event);
    recorder.add(event);
  }

  private static void recordHttpServerException(Event event, HttpServletRequest req, Exception exception) {
    event.setException(exception);
    clearLastEvent(req, event);
    recorder.add(event);
  }

  /* #region HttpServlet.service */

  @ArgumentArray
  @ExcludeReceiver
  @HookClass("javax.servlet.http.HttpServlet")
  public static void service(Event event, Object[] args) {
    if (args.length != 2) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    HttpServerRequest.recordHttpServerRequest(event, req);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service")
  public static void serviceJakarta(Event event, Object[] args) {
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
    doServiceReturn(event, args);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service", methodEvent = MethodEvent.METHOD_RETURN)
  public static void serviceJakarta(Event event, Object returnValue, Object[] args) {
    doServiceReturn(event, args);
  }

  private static void doServiceReturn(Event event, Object[] args) {
    if (args.length != 2) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    if (isSpringRequest(req)) {
      logger.trace("service, isSpringRequest");
      addSpringPath(req);
    } else {
      logger.trace("service, not isSpringRequest");
      logger.trace("request attributes: {}", () -> req.getAttributeNames());
    }

    HttpServletResponse res = new HttpServletResponse(args[1]);
    recordHttpServerResponse(event, req, res);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "javax.servlet.http.HttpServlet", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void service(Event event, Exception exception, Object[] args) {
    if (args.length != 2) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    recordHttpServerException(event, req, exception);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "jakarta.servlet.http.HttpServlet", method = "service", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void serviceJakarta(Event event, Exception exception, Object[] args) {
    if (args.length != 2) {
      return;
    }

    HttpServletRequest req = new HttpServletRequest(args[0]);
    recordHttpServerException(event, req, exception);
  }

  /* #endregion HttpServlet.service */

  private static boolean isSpringRequest(HttpServletRequest req) {
    return req.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE) != null;
  }

  private static void addSpringPath(HttpServletRequest req) {
    final String uri = (String) req.getRequestURI();
    if (uri != null) {
      final Event lastEvent = (Event) req.getAttribute(LAST_EVENT_KEY);
      if (lastEvent == null || lastEvent.httpServerRequest == null) {
        return;
      }
      // Allow updating the event. It'll get frozen again when it gets added to
      // the session.
      lastEvent.defrost();

      Map<String, ?> uriTemplateVariables = getTemplateVariables(req);
      if (uriTemplateVariables != null) {
        // If it has template variables, it's parameterized.
        final String pattern = (String) req.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE);
        lastEvent.httpServerRequest.setNormalizedPath(pattern);

        addMessageParams(uriTemplateVariables, lastEvent);
      }

      recorder.addEventUpdate(lastEvent);
    }
  }

  private static void addMessageParams(Map<String, ?> uriTemplateVariables, final Event lastEvent) {
    for (Map.Entry<String, ?> param : uriTemplateVariables.entrySet()) {
      lastEvent.addMessageParam(param.getKey(), param.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, ?> getTemplateVariables(HttpServletRequest req) {
    return (Map<String, ?>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
  }
}
