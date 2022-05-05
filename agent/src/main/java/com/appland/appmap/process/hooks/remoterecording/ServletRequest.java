package com.appland.appmap.process.hooks.remoterecording;

import java.io.IOException;
import java.io.PrintWriter;
import com.appland.appmap.record.Recording;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;

class ServletRequest implements RemoteRecordingRequest {
  private HttpServletRequest req;
  private HttpServletResponse res;

  ServletRequest(HttpServletRequest req, HttpServletResponse res) {
    this.req = req;
    this.res = res;
  }

  public String getRequestURI() { 
    return req.getRequestURI();
  }

  public String getMethod() {
    return req.getMethod();
  }

  public void setStatus(int status) {
    res.setStatus(status);
  }
  
  public void writeJson(String responseJson) throws IOException {
    res.setContentType("application/json");
    res.setContentLength(responseJson.length());
    res.setStatus(HttpServletResponse.SC_OK);

    PrintWriter writer = res.getWriter();
    writer.write(responseJson);
    writer.flush();
  }

  public void writeRecording(Recording recording) throws IOException {
    res.setContentType("application/json");
    res.setContentLength(recording.size());
    recording.readFully(true, res.getWriter());
  }
}
