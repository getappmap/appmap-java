package com.appland.appmap.process.hooks;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;

public class Proxy {
  static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static final Map<Method, ProxyHooks> allProxyHooks = new HashMap<>();

  @ArgumentArray
  @HookClass("java.lang.reflect.InvocationHandler")
  public static void invoke(Event event, Object receiver, Object[] args) {
    Object proxy = args[0];
    Method method = (Method) args[1];
    Object[] methodArgs = (Object[]) args[2];
    logger.trace("receiver: {}, proxy: {}, method: {}, method args: {}", receiver::toString, proxy::toString,
        () -> String.format("%s@%x", method, method.hashCode()),
        () -> methodArgs != null ? methodArgs.toString() : "null");

    ProxyHooks proxyHooks = allProxyHooks.get(method);
    if (proxyHooks == null) {
      proxyHooks = ProxyHooks.build(method, methodArgs);
    }
    if (proxyHooks == null) {
      // Still null => method not hooked
      return;
    }
    allProxyHooks.put(method, proxyHooks);

    proxyHooks.invokeCall(proxy, method, methodArgs);
  }

  @ArgumentArray
  @HookClass(value = "java.lang.reflect.InvocationHandler", method = "invoke",
      methodEvent = MethodEvent.METHOD_RETURN)
  public static void invokeReturn(Event event, Object receiver, Object ret, Object[] args) {
    Object proxy = args[0];
    Method method = (Method) args[1];
    Object[] methodArgs = (Object[]) args[2];
    logger.trace("receiver: {}, proxy: {}, method: {}, method args: {}", receiver::toString, proxy::toString,
        () -> String.format("%s@%x", method, method.hashCode()),
        () -> methodArgs != null ? methodArgs.toString() : "null");

    ProxyHooks proxyHooks = allProxyHooks.get(method);
    if (proxyHooks != null) {
      proxyHooks.invokeReturn(proxy, ret, methodArgs);
    }
  }

  @ArgumentArray
  @HookClass(value = "java.lang.reflect.InvocationHandler", method = "invoke",
      methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void invokeExc(Event event, Object receiver, Throwable exc, Object[] args) {
    Object proxy = args[0];
    Method method = (Method) args[1];
    Object[] methodArgs = (Object[]) args[2];
    logger.trace("receiver: {}, proxy: {}, method: {}, method args: {}", receiver::toString, proxy::toString,
        () -> String.format("%s@%x", method, method.hashCode()),
        () -> methodArgs != null ? methodArgs.toString() : "null");

    ProxyHooks proxyHooks = allProxyHooks.get(method);
    if (proxyHooks != null) {
      proxyHooks.invokeExc(proxy, exc, methodArgs);
    }
  }
}
