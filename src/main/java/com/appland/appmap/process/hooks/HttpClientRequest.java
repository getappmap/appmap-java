package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Unique("http_client_request")
public class HttpClientRequest {

  private static final Recorder recorder = Recorder.getInstance();

  // @HookClass(value = "java.net.HttpURLConnection")
  public static void connect(Event event, HttpURLConnection httpURLConnection) {
    //TODO: ReflectiveType can be used with HttpURLConnection
    URL url = httpURLConnection.getURL();
    event.setHttpClientRequest(httpURLConnection.getRequestMethod(), url.getHost(), url.getProtocol());
    recorder.add(event);
  }

  // @CallbackOn(MethodEvent.METHOD_RETURN)
  // @HookClass(value = "java.net.HttpURLConnection")
  public static void connect(Event event, HttpURLConnection httpURLConnection, Object ret) throws IOException {
    event.setHttpClientResponse(httpURLConnection.getResponseCode(), httpURLConnection.getContentType());
    recorder.add(event);
  }

}
