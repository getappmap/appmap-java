package com.appland.appmap.process.hooks.http;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.process.hooks.RequestRecording;
import com.appland.appmap.reflect.DynamicReflectiveType;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.ServletRequestEvent;

public class ServletListener implements InvocationHandler {
  public static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static final String PACKAGE_NAME = MethodHandles.lookup().lookupClass().getPackage().getName();
  public static final String RECORDING_ATTRIBUTE = PACKAGE_NAME + ".recording";

  private ServletListener() {
  }

  public static Object build(ClassLoader cl) {
    return DynamicReflectiveType.build(new ServletListener(), cl, "javax.servlet.ServletRequestListener",
        "jakarta.servlet.ServletRequestListener");
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if (methodName.equals("requestInitialized")) {
      HttpServletRequest servletRequest = new ServletRequestEvent(args[0]).getServletRequest();
      RequestRecording.start(servletRequest);
    } else if (methodName.equals("requestDestroyed")) {
      HttpServletRequest servletRequest = new ServletRequestEvent(args[0]).getServletRequest();
      if (!servletRequest.isForStaticResource()) {
        RequestRecording.stop(servletRequest);
      } else {
        RequestRecording.abort();
      }

    }
    else {
      throw new InternalError("unhandled method" + methodName);
    }

    return null;
  }
}