package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.CallbackOn;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;

import java.util.Map;


/**
 * Hooks to capture @{code message} data.
 */
public class Message {
  private static final Recorder recorder = Recorder.getInstance();

  /**
   * Capture message params from Spring.
   */
  @CallbackOn(MethodEvent.METHOD_RETURN)
  @ExcludeReceiver
  @HookClass("org.springframework.util.PathMatcher")
  public static void extractUriTemplateVariables(Event event,
                                                 Object returnVal,
                                                 String pattern,
                                                 String path) {
    final Event lastEvent = recorder.getLastEvent();
    if (lastEvent == null) {
      return;
    }

    if (lastEvent.httpRequest == null) {
      return;
    }

    Map<String, String> pathParams = (Map<String, String>)returnVal;
    for (Map.Entry<String, String> param : pathParams.entrySet()) {
      lastEvent.addMessageParam(param.getKey(), param.getValue());
    }
    final String normalizedPath = pattern.replace('{', ':').replace("}", "");
    lastEvent.httpRequest.setNormalizedPath(normalizedPath);
  }
}
