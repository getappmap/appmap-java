package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.util.Logger;

import java.util.Map;

/**
 * Hooks to capture @{code message} data.
 */
public class Message {
  private static final Recorder recorder = Recorder.getInstance();

  /**
   * Capture message params from Spring.
   */
  @ExcludeReceiver
  @HookClass(value = "org.springframework.util.PathMatcher",  methodEvent = MethodEvent.METHOD_RETURN)
  public static void extractUriTemplateVariables(Event event,
                                                 Object returnVal,
                                                 String pattern,
                                                 String path) {
    final Event lastEvent = recorder.getLastEvent();
    if (lastEvent == null) {
      return;
    }

    if (lastEvent.httpServerRequest == null) {
      return;
    }

    if (lastEvent.frozen()) {
      Logger.printf("Won't set message params or normalized path on event %d because it's already frozen\n", lastEvent.id);
      return;
    }

    addMessageParams(returnVal, lastEvent);

    // KEG: I can suggest a more robust way to hook this information.
    // Spring framework's RequestMappingInfoHandlerMapping sets the following request attributes:
    //
    // request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
    // request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, decodedUriVariables);
    // request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodedUriVariables);
    //
    // Try hooking javax.servlet.ServletRequest#setAttribute instead.

    final String normalizedPath = pattern.replace('{', ':').replace("}", "");
    lastEvent.httpServerRequest.setNormalizedPath(normalizedPath);
  }

  @SuppressWarnings("unchecked")
  private static void addMessageParams(Object returnVal, final Event lastEvent) {
    Map<String, String> pathParams = (Map<String, String>)returnVal;
    for (Map.Entry<String, String> param : pathParams.entrySet()) {
      lastEvent.addMessageParam(param.getKey(), param.getValue());
    }
  }
}
