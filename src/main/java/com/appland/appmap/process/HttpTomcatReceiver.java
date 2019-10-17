package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class HttpTomcatReceiver implements IEventProcessor {
  private Event onMethodInvocation(Event event) {
    Value requestParam = event.popParameter("req");
    if (requestParam == null) {
      return null;
    }
    event.popParameter("resp");

    HttpServletRequest request = requestParam.get();
    event.setHttpServerRequest(request.getMethod(), request.getRequestURI(), request.getProtocol());

    return event;
  }

  private Event onMethodReturn(Event event) {
    // Value responseParam = event.getParameter("resp");
    // if (responseParam == null) {
    //   return null;
    // }

    // HttpServletResponse response = responseParam.get();
    // HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response);
    // event.setHttpServerResponse(responseWrapper.getStatus());

    return event;
  }

  @Override
  public Event processEvent(Event event) {
    if (event.event.equals("call")) {
      return this.onMethodInvocation(event);
    } else if (event.event.equals("return")) {
      return this.onMethodReturn(event);
    }

    return null;
  }
}
