package com.appland.appmap.process.hooks.remoterecording;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.appland.appmap.config.Properties;
import com.appland.appmap.reflect.DynamicReflectiveType;
import com.appland.appmap.reflect.HttpServletRequest;
import com.appland.appmap.reflect.HttpServletResponse;
import com.appland.appmap.reflect.ReflectiveType;

public class RemoteRecordingFilter implements InvocationHandler {
  private static class FilterChain extends ReflectiveType {
    static String DO_FILTER = "doFilter";

    public FilterChain(Object self) {
      super(self);
      if (!addMethod(DO_FILTER, "javax.servlet.ServletRequest", "javax.servlet.ServletResponse")
          && !addMethod(DO_FILTER, "jakarta.servlet.ServletRequest", "jakarta.servlet.ServletResponse")) {
        throw new InternalError("No find doFilter method");
      }
    }

    public void doFilter(Object req, Object resp) {
      invokeVoidMethod(DO_FILTER, req, resp);
    }
  }

  private RemoteRecordingFilter() {
  }

  public static Object build() {
    return DynamicReflectiveType.build(new RemoteRecordingFilter(), "javax.servlet.Filter",
        "jakarta.servlet.Filter", "org.springframework.core.Ordered");
  }

  private static void doFilter(Object[] args) {
    final HttpServletRequest req = new HttpServletRequest(args[0]);
    final HttpServletResponse res = new HttpServletResponse(args[1]);
    final FilterChain chain = new FilterChain(args[2]);

    if (!Properties.RecordingRemote
        || !RemoteRecordingManager.service(new ServletRequest(req, res))) {
      chain.doFilter(args[0], args[1]);
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if (methodName.equals("doFilter")) {
      doFilter(args);
    } else if (methodName.equals("getOrder")) {
      Class<?> methodClass = method.getDeclaringClass();
      Field highestPrecedence = methodClass.getField("HIGHEST_PRECEDENCE");
      return highestPrecedence.getInt(methodClass);
    } else if (methodName.equals("init") || methodName.equals("destroy")) {
      // nothing to do for these, but they need to be implemented
    } else {
      throw new InternalError("unhandled method " + methodName);
    }

    return null;
  }
}