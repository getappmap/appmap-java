package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.reflect.apache.HttpResponse;
import com.appland.appmap.reflect.apache.HttpUriRequest;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;

@Unique("http_client_request")
public class HttpClientRequest {

  private static final Recorder recorder = Recorder.getInstance();

  /*
   * See https://hc.apache.org/httpcomponents-client-4.5.x/index.html for a
   * description of HttpClient.
   */
  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.client.HttpClient")
  public static void execute(Event event, Object[] args) {
    HttpUriRequest req = new HttpUriRequest(args[0]);
    event.setHttpClientRequest(req.getMethod(), req.getURI().toString());
    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.client.HttpClient", methodEvent = MethodEvent.METHOD_RETURN)
  public static void execute(Event event, Object ret, Object[] args) {

    HttpResponse res = new HttpResponse(ret);
    event.setHttpClientResponse(res.getStatusCode(), res.getContentType());
    recorder.add(event);
  }
}
