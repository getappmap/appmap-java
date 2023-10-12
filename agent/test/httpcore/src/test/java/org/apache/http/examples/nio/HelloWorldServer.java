package org.apache.http.examples.nio;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.entity.StringEntity;
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;

import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.nio.protocol.BasicAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.appland.appmap.annotation.Labels;

public class HelloWorldServer {
  static class HelloWorldHandler implements HttpRequestHandler {
    @Labels({"say", "Hello"})
    String sayHello() { 
      return "<body><html><h1>Hello World!</h1></html></body>";
    }

    @Labels({"handler"})
    public void handle(
            final HttpRequest request,
            final HttpResponse response,
        final HttpContext context) throws HttpException, IOException {
      RequestLine rl = request.getRequestLine();

      if (rl.getMethod().equals("DELETE") && rl.getUri().equals("/exit")) {
        System.exit(0);
      }
      response.setEntity(new StringEntity(sayHello()));
      response.setStatusCode(200);
    }
  }

  @Labels({"server", "runner"})
  public HttpServer run(int port) throws IOException, InterruptedException {
    final HttpServer server = ServerBootstrap.bootstrap()
      .setListenerPort(port)
      .setServerInfo("HelloWorld/1.1")
      .setExceptionLogger(ExceptionLogger.STD_ERR)
      .registerHandler("*", new BasicAsyncRequestHandler(new HelloWorldHandler()))
      .create();

      server.start();
      server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

      return server;
  }

  public static void main(String[] argv) {
    final int port = Integer.parseInt(argv[0]);

    try {
      final HttpServer server = new HelloWorldServer().run(port);

      Runtime.getRuntime().addShutdownHook(new Thread() {
          @Override
          public void run() {
              server.shutdown(5, TimeUnit.SECONDS);
          }
      });
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
