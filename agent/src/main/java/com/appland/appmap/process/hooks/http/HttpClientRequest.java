package com.appland.appmap.process.hooks.http;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.reflect.apache.HttpHost;
import com.appland.appmap.reflect.apache.HttpRequest;
import com.appland.appmap.reflect.apache.HttpResponse;
import com.appland.appmap.reflect.apache.HttpUriRequest;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Signature;
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
  @Signature({ "org.apache.http.client.methods.HttpUriRequest" })
  @Signature({ "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.protocol.HttpContext" })
  public static void execute(Event event, Object[] args) {
    HttpUriRequest req = new HttpUriRequest(args[0]);
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(req.getURI());
    execute(event, req.getMethod(), builder);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.client.HttpClient", method = "execute")
  @Signature({ "org.apache.http.HttpHost", "org.apache.http.HttpRequest" })
  @Signature({ "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext" })
  public static void executeOnHost(Event event, Object[] args) {
    HttpHost host = new HttpHost(args[0]);
    HttpRequest req = new HttpRequest(args[1]);
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(req.getUri());
    UriComponentsBuilder hostBuilder = UriComponentsBuilder.fromUriString(host.toURI());
    builder.uriComponents(hostBuilder.build());
    execute(event, req.getMethod(), builder);
  }

  private static void execute(Event event, String method, UriComponentsBuilder builder) {

    UriComponents withQuery = builder.build(true);
    UriComponents noQuery = builder.replaceQuery(null).build(true);

    event.setHttpClientRequest(method, noQuery.toString());

    event.setParameters(null);
    Set<Entry<String, List<String>>> entrySet = withQuery.getQueryParams().entrySet();
    for (Map.Entry<String, List<String>> param : entrySet) {
      List<String> allValues = param.getValue();
      String[] values = allValues.toArray(new String[0]);
      event.addMessageParam(param.getKey(), values.length > 0 ? values[0] : "");
    }

    recorder.add(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.client.HttpClient", methodEvent = MethodEvent.METHOD_RETURN)
  @Signature({ "org.apache.http.client.methods.HttpUriRequest" })
  @Signature({ "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.protocol.HttpContext" })
  @Signature({ "org.apache.http.HttpHost", "org.apache.http.HttpRequest" })
  @Signature({ "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext" })
  public static void execute(Event event, Object ret, Object[] args) {
    HttpResponse res = new HttpResponse(ret);
    event.setHttpClientResponse(res.getStatusCode(), res.getContentType());
    event.setParameters(null);
    recorder.add(event);
  }
}