package com.appland.appmap.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;

public class DynamicReflectiveType {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private static class ReflectiveHandler implements InvocationHandler {
    private InvocationHandler delegate;

    ReflectiveHandler(InvocationHandler delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Class<?> declaringClass = method.getDeclaringClass();

      if (declaringClass == Object.class) {
        if (method.getName().equals("hashCode")) {
          return Integer.valueOf(System.identityHashCode(proxy));
        } else if (method.getName().equals("equals")) {
          return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
        } else if (method.getName().equals("toString")) {
          return proxy.getClass().getName() + '@' +
              Integer.toHexString(proxy.hashCode());
        } else {
          throw new InternalError(
              "unexpected Object method dispatched: " + method);
        }
      }

      return delegate.invoke(proxy, method, args);

    }
  }

  public static Object build(InvocationHandler handler, ClassLoader cl, String... interfaceNames) {
    ArrayList<Class<?>> interfaces = new ArrayList<Class<?>>();
    for (String interfaceName : interfaceNames) {
      Class<?> cls;
      if ((cls = tryClass(cl, interfaceName)) != null) {
        interfaces.add(cls);
      }
    }

    if (interfaces.size() == 0) {
      logger.debug("None of {} found", () -> String.join(",", interfaceNames));
      return null;
    }

    return Proxy.newProxyInstance(
        cl,
        interfaces.toArray(new Class[0]),
        new ReflectiveHandler(handler));
  }

  private static Class<?> tryClass(ClassLoader cl, String className) {
    try {
      return cl.loadClass(className);
    } catch (ClassNotFoundException e) {
      logger.trace(e);
    }
    return null;
  }
}
