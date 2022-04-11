package com.appland.appmap.process.hooks.remoterecording;

import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.record.Recording;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.util.Logger;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;

import org.apache.http.protocol.HttpContext;

import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;

public class HttpCoreRequest implements RemoteRecordingRequest {
  private static final boolean debug = Properties.DebugHttp;

  private HttpRequest req;
  private HttpResponse res;

  HttpCoreRequest(HttpRequest req, HttpResponse res) {
    this.req = req;
    this.res = res;
  }

  public String getRequestURI() {
    return req.getRequestLine().getUri();
  }

  public String getMethod() {
    return req.getRequestLine().getMethod();
  }

  public void setStatus(int status) {
    res.setStatusCode(status);
  }

  public void writeJson(String responseJson) throws IOException {
    final StringEntity entity = new StringEntity(responseJson);
    entity.setContentType("application/json");
    res.setEntity(entity);
  }

  public String toString() {
    return "HttpCoreRequest, req: " + req + " res: " + res;
  }

  public void writeRecording(Recording recording) throws IOException {
    final InputStreamEntity entity = new InputStreamEntity(
      recording.asInputStream()
    );
    entity.setContentType("application/json");
    res.setEntity(entity);
  }


}
