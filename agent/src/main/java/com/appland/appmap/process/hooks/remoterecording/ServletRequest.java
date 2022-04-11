package com.appland.appmap.process.hooks.remoterecording;

import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.process.conditions.RecordCondition;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.reflect.FilterChain;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.transform.annotations.*;
import com.appland.appmap.util.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static com.appland.appmap.util.StringUtil.*;

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
