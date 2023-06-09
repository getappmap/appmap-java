package com.appland.appmap.process.hooks.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.ISystem;
import com.appland.appmap.transform.annotations.MethodEvent;
public class HttpCoreHooks {
  private static Map<String, String> getHeaderMap(Header[] headers) {
    HashMap<String, String> ret = new HashMap<String, String>();
    for (Header h : headers) {
      ret.put(h.getName(), h.getValue());
    }

    return ret;
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.protocol.HttpRequestHandler", method = "handle")
  public static void handleSync(Event event, Object[] args)
      throws IOException, HttpException, ExitEarly {
    HttpRequest req = (HttpRequest) args[0];
    final RequestLine rl = req.getRequestLine();
    HttpServerRequest.recordHttpServerRequest(event,
        rl.getMethod(), rl.getUri(), rl.getProtocolVersion().toString(),
        getHeaderMap(req.getAllHeaders())); // TODO: add params
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.protocol.HttpRequestHandler", method = "handle", position = ISystem.HOOK_POSITION_LAST, methodEvent = MethodEvent.METHOD_RETURN)
  public static void postHandleSync(Event event, Object ret, Object[] args)
      throws IOException, HttpException, ExitEarly {
    HttpResponse res = (HttpResponse) args[1];
    HttpServerRequest.recordHttpServerResponse(event, null, res.getStatusLine().getStatusCode(),
        getHeaderMap(res.getAllHeaders()));
  }

}
