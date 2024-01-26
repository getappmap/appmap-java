package com.appland.appmap.runtime;

import java.lang.reflect.Method;

/*
 * HooksFunctions acts as a bridge between instrumented code and the rest of the agent. It gets
 * loaded into the boot class loader, making it visible to all classes, regardless of how the app
 * structures its class loader hierarchy.
 *
 * On startup, the agent installs the onMethodCall and onMethodReturn hooks. At runtime,
 * instrumented code calls those hooks to add "call" and "return" events.
 */
public class HookFunctions {
  public interface OnCallConsumer {
    void accept(int callOrdinal, Object receiver, Object[] args);
  }

  public interface OnReturnConsumer {
    void accept(int returnOrdinal, int excOrdinal, Object receiver, Method method, Object[] args,
        Object returnValue,
        Throwable exception) throws Throwable;
  }

  public static OnCallConsumer onMethodCall;
  public static OnReturnConsumer onMethodReturn;
}
