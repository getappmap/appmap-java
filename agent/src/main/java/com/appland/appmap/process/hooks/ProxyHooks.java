package com.appland.appmap.process.hooks;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.EventTemplateRegistry;
import com.appland.appmap.transform.ClassFileTransformer;
import com.appland.appmap.transform.annotations.HookFactory;
import com.appland.appmap.transform.annotations.HookSite;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.util.AppMapClassPool;
import com.appland.appmap.util.ClassUtil;
import com.appland.appmap.util.FullyQualifiedName;

import javassist.CtBehavior;
import javassist.NotFoundException;

public class ProxyHooks {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  static final ClassFileTransformer transformer =
      new ClassFileTransformer("proxy hooks", HookFactory.ALL_HOOKS_FACTORY);
  private final HookSite callHook;
  private final HookSite returnHook;
  private final HookSite excHook;

  private ProxyHooks(List<HookSite> hookSites) {
    callHook = Objects.requireNonNull(hookSites.get(MethodEvent.METHOD_INVOCATION.getIndex()));
    returnHook = Objects.requireNonNull(hookSites.get(MethodEvent.METHOD_RETURN.getIndex()));
    excHook = Objects.requireNonNull(hookSites.get(MethodEvent.METHOD_EXCEPTION.getIndex()));
  }

  static ProxyHooks build(Method method, Object[] methodArgs) {
    // Only hook methods that are matched by the config.
    FullyQualifiedName fqn = new FullyQualifiedName(method);
    if (AppMapConfig.get().includes(fqn) == null) {
      return null;
    }

    AppMapClassPool.acquire(Thread.currentThread().getContextClassLoader());
    try {
      String[] argTypes =
          Arrays.stream(method.getParameterTypes()).map(a -> a.getName()).toArray(String[]::new);
      CtBehavior behavior = ClassUtil.getDeclaredMethod(method.getDeclaringClass().getName(),
          method.getName(), argTypes);
      logger.trace("method: '{},'{}' behavior: {}", method::toString, method::toGenericString,
          behavior::getLongName);
      final List<HookSite> hookSites = transformer.getHookSites(behavior);

      logger.trace("hookSites: {}", hookSites);
      if (hookSites == null) {
        // not hooked
        return null;
      } else {
        logger.debug("{} hooks for method: '{},'{}' behavior: {}", hookSites::size,
            method::toString, method::toGenericString, behavior::getLongName);
      }
      return new ProxyHooks(hookSites);
    } catch (NotFoundException e) {
      logger.warn(e);
    } finally {
      AppMapClassPool.release();
    }

    return null;
  }

  void invokeCall(Object proxy, Method method, Object[] methodArgs) {
    logger.trace("callHook: {}, method event: {}, method: {}", callHook::toString,
        callHook::getMethodEvent, method::getName);
    Event event = EventTemplateRegistry.get().buildCallEvent(callHook.getBehaviorOrdinal());
    MethodCall.handle(event, proxy, methodArgs);
  }

  void invokeReturn(Object proxy, Object ret, Object[] methodArgs) {
    logger.trace("returnHook: {}, method event: {}, ret: {}", returnHook::toString,
        returnHook::getMethodEvent, () -> ret != null ? ret.toString() : "void");
    Event event = EventTemplateRegistry.get().buildReturnEvent(returnHook.getBehaviorOrdinal());
    MethodReturn.handle(event, proxy, ret, methodArgs);
  }

  void invokeExc(Object proxy, Throwable exc, Object[] methodArgs) {
    logger.trace("excHook: {}, method event: {}", excHook::toString, excHook::getMethodEvent);
    Event event = EventTemplateRegistry.get().buildReturnEvent(excHook.getBehaviorOrdinal());
    MethodException.handle(event, proxy, exc, methodArgs);
  }

}
