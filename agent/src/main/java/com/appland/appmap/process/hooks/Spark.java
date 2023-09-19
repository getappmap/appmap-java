package com.appland.appmap.process.hooks;

import static com.appland.appmap.util.ClassUtil.safeClassForName;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.ExitEarly;
import com.appland.appmap.process.hooks.http.HttpServerRequest;
import com.appland.appmap.process.hooks.remoterecording.RemoteRecordingManager;
import com.appland.appmap.process.hooks.remoterecording.ServletRequest;
import com.appland.appmap.reflect.DynamicReflectiveType;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.reflect.ReflectiveType;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookClass;

public class Spark {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private static class HandlerWrapper extends ReflectiveType {
    private static String SET_HANDLER = "setHandler";

    public HandlerWrapper(Object self) {
      super(self);
      addMethod(SET_HANDLER, "org.eclipse.jetty.server.Handler");
    }

    public void setHandler(Object handler) {
      invokeVoidMethod(SET_HANDLER, handler);
    }

  }

  private static class JettyRequest extends ReflectiveType {
    private static String SET_HANDLED = "setHandled";

    public JettyRequest(Object self) {
      super(self);
      addMethod(SET_HANDLED, Boolean.TYPE);
    }

    public void setHandled(boolean handled) {
      invokeVoidMethod(SET_HANDLED, handled);
    }
  }

  private static class Handler extends DynamicReflectiveType implements InvocationHandler {
    private Object wrapper;

    private Handler(Object wrapper) {
      this.wrapper = wrapper;
    }

    static Object build(Object handler) {
      ClassLoader cl = handler.getClass().getClassLoader();
      try {
        Object wrapper = safeClassForName(cl, "org.eclipse.jetty.server.handler.HandlerWrapper")
            .getConstructor()
            .newInstance();
        new HandlerWrapper(wrapper)
            .setHandler(handler);
        return DynamicReflectiveType.build(new Handler(wrapper), cl, "org.eclipse.jetty.server.Handler");
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        // Should never happen
        logger.error(e);
        throw new InternalError(e);
      }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      if (name.equals("handle")) {
        JettyRequest jettyReq = new JettyRequest(args[1]);
        HttpServletRequest req = new HttpServletRequest(args[2]);
        HttpServletResponse resp = new HttpServletResponse(args[3]);

        String requestURI = req.getRequestURI();
        logger.trace(new Exception(), "handling {}", requestURI);
        if (requestURI.equals("/_appmap/record")) {
          if (RemoteRecordingManager.service(new ServletRequest(req, resp))) {
            jettyReq.setHandled(true);
            return null;
          }
        }

        RequestRecording.start(req);
        Event http_server_request = Event.functionCallEvent();
        HttpServerRequest.recordHttpServerRequest(http_server_request, req);

        try {
          method.invoke(wrapper, args);
          req.setAttribute(HttpServerRequest.STATUS_ATTRIBUTE, Integer.valueOf(resp.getStatus()));
          Event http_server_response = Event.functionReturnEvent();
          HttpServerRequest.recordHttpServerResponse(http_server_response, req, resp);
          return null;
        } finally {
          RequestRecording.stop(req);
        }
      }
      logger.trace("wrapper: {}, invoking {}", wrapper, method);
      return method.invoke(wrapper, args);
    }
  }

  private static class HandlerList extends ReflectiveType {
    private static String GET_HANDLERS = "getHandlers";

    public HandlerList(Object self) {
      super(self);
      addMethods(GET_HANDLERS);
    }

    public Object[] getHandlers() {
      return (Object[]) invokeObjectMethod(GET_HANDLERS);
    }
  }

  @HookClass(value = "org.eclipse.jetty.server.handler.HandlerWrapper")
  @ArgumentArray
  public static void setHandler(Event event, Object receiver, Object[] args) {
    // TODO: consider all inherited methods in ClassFileTransformer.transform?

    // If we looked at all the methods returned by CtClass.getMethods, in
    // addition to those returned by CtClass.getDeclaredBehaviors, we wouldn't
    // have to do things like this:
    if (!receiver.getClass().getName().equals("org.eclipse.jetty.server.Server")) {
      return;
    }

    Object handler = args[0];
    // Spark will set the server's handler to be either a JettyHandler, or a
    // HandlerList that contains a JettyHandler.
    String argClass = handler.getClass().getName();
    String jettyHandlerClass = "spark.embeddedserver.jetty.JettyHandler";
    if (!argClass.equals(jettyHandlerClass)) {
      if (argClass.equals("org.eclipse.jetty.server.handler.HandlerList")) {
        HandlerList hl = new HandlerList(handler);
        boolean match = Arrays.stream(hl.getHandlers()).anyMatch(h -> h.getClass().getName().equals(jettyHandlerClass));
        if (!match) {
          return;
        }
      }
    }
    HandlerWrapper server = new HandlerWrapper(receiver);
    logger.trace("handler: {}", handler);
    server.setHandler(Handler.build(handler));

    // We just set the handler, don't continue with the method
    throw new ExitEarly();
  }

}
