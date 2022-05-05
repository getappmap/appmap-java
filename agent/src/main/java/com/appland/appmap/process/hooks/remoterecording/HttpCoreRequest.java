package com.appland.appmap.process.hooks.remoterecording;

import java.io.IOException;
import com.appland.appmap.record.Recording;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;

public class HttpCoreRequest implements RemoteRecordingRequest {
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
