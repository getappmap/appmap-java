package com.appland.appmap.process.hooks.remoterecording;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.record.Recording;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.CallbackOn;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.ISystem;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.util.Logger;
import com.appland.appmap.process.hooks.HttpServerRequest;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;

import org.apache.http.protocol.HttpContext;

import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;

public class HttpCoreHooks {
  private static Boolean debug = Properties.DebugHttp;

  private static Map<String, String> getHeaderMap(Header[] headers) {
    HashMap<String, String> ret = new HashMap<String, String>();
    for (Header h: headers) {
      ret.put(h.getName(), h.getValue());
    }

    return ret;
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value="org.apache.http.protocol.HttpRequestHandler", method="handle")
  public static void handleSync(Event event, Object[] args)
    throws IOException, HttpException, ExitEarly {
    HttpRequest req = (HttpRequest) args[0];
    final RequestLine rl = req.getRequestLine();
    HttpServerRequest.recordHttpServerRequest(event, 
      rl.getMethod(), rl.getUri(), rl.getProtocolVersion().toString(),
      getHeaderMap(req.getAllHeaders()),
      null); // TODO: add params
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value = "org.apache.http.protocol.HttpRequestHandler", method="handle", position = ISystem.HOOK_POSITION_LAST)
  @CallbackOn(value = MethodEvent.METHOD_RETURN)
  public static void postHandleSync(Event event, Object ret, Object[] args)
    throws IOException, HttpException, ExitEarly {
    HttpResponse res = (HttpResponse) args[1];
    HttpServerRequest.recordHttpServerResponse(event, res.getStatusLine().getStatusCode(), getHeaderMap(res.getAllHeaders()));
  }

}
