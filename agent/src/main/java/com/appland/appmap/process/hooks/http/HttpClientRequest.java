package com.appland.appmap.process.hooks.http;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.reflect.apache.HttpRequest;
import com.appland.appmap.reflect.apache.HttpResponse;
import com.appland.appmap.reflect.apache.HttpUriRequest;
import com.appland.appmap.reflect.apache.NameValuePair;
import com.appland.appmap.reflect.apache.URIBuilder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Signature;
import com.appland.appmap.transform.annotations.Unique;

@Unique("http_client_request")
public class HttpClientRequest {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

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
    URIBuilder builder = newBuilder(req.getURI());
    execute(event, req.getMethod(), builder);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.client.HttpClient", method = "execute")
  @Signature({ "org.apache.http.HttpHost", "org.apache.http.HttpRequest" })
  @Signature({ "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext" })
  public static void executeOnHost(Event event, Object[] args) {
    HttpRequest req = new HttpRequest(args[1]);
    try {
      URI hostURI = new URI(args[0].toString());
      URIBuilder builder = newBuilder(new URI(req.getUri()))
          .setScheme(hostURI.getScheme())
          .setHost(hostURI.getHost())
          .setPort(hostURI.getPort());
      execute(event, req.getMethod(), builder);
    } catch (URISyntaxException e) {
      logger.warn(e, "req: {}");
    }
  }

  private static void execute(Event event, String method, URIBuilder builder) {
    List<?> params = builder.getQueryParams();

    String noQuery = builder.removeQuery().build().toString();
    event.setHttpClientRequest(method, noQuery);

    event.setParameters(null);
    for (Object param : params) {
      NameValuePair nvp = new NameValuePair(param);
      event.addMessageParam(nvp.getName(), nvp.getValue());
    }

    recorder.add(event);
  }

  static private URIBuilder newBuilder(URI uri) {
    try {
      Class<?> cls = Class.forName("org.apache.http.client.utils.URIBuilder");
      Constructor<?> ctor = cls.getConstructor(java.net.URI.class);
      return new URIBuilder(ctor.newInstance(uri));
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      logger.error(e, "failed creating a URIBuilder");
      // This shouldn't ever happen: we've hooked a method in
      // org.apachage.http.client, so URIBuilder should always be available.
      throw new InternalError(e);
    }
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