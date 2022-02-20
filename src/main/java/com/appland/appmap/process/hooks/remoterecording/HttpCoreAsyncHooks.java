package com.appland.appmap.process.hooks.remoterecording;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;

import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.util.Logger;

public class HttpCoreAsyncHooks {
  private static final boolean debug = Properties.DebugHttp;

  static class AppMapHandler implements HttpAsyncRequestHandler<HttpRequest> {
      public HttpAsyncRequestConsumer<HttpRequest> processRequest(
              final HttpRequest request,
              final HttpContext context) {
          // Buffer request content in memory for simplicity
          return new BasicAsyncRequestConsumer();
      }

      public void handle(
              final HttpRequest request,
              final HttpAsyncExchange httpexchange,
              final HttpContext context) throws HttpException, IOException {
          final HttpResponse response = httpexchange.getResponse();
          RemoteRecordingManager.service(new HttpCoreRequest(request, response));
          httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
      }
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookClass("org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper")
  public static void lookup(Event event, Object[] args) throws ExitEarly {
    HttpRequest req = (HttpRequest)args[0];
    if (debug) {
      Logger.println("HttpCoreAsyncRequestHandler.lookup: " + req);
    }

    if (req.getRequestLine().getUri().startsWith(RemoteRecordingManager.RecordRoute)) {
      throw new ExitEarly(new AppMapHandler());
    }
  } 
  
  @ArgumentArray
  @ExcludeReceiver
  @HookClass(value="org.apache.http.nio.protocol.BasicAsyncRequestHandler", method="handle")
  public static void handleAsync(Event event, Object[] args) throws IOException, HttpException, ExitEarly {
    final HttpRequest req = (HttpRequest)args[0];
    final HttpAsyncExchange httpexchange = (HttpAsyncExchange)args[1];
    final HttpResponse res = httpexchange.getResponse();
    final boolean handled = RemoteRecordingManager.service(new HttpCoreRequest(req, res));
    if (handled) {
      httpexchange.submitResponse(new BasicAsyncResponseProducer(res));
      throw new ExitEarly();
    }
  }
}
