package com.appland.appmap.transform.instrumentation;

import java.lang.reflect.Method;

import com.appland.appmap.process.ThreadLock;
import com.appland.appmap.process.hooks.MethodCall;
import com.appland.appmap.process.hooks.MethodException;
import com.appland.appmap.process.hooks.MethodReturn;
import com.appland.appmap.record.EventTemplateRegistry;
import com.appland.appmap.transform.annotations.AppMapAppMethod;
import com.appland.appmap.transform.annotations.MethodEvent;

import net.bytebuddy.asm.Advice.AllArguments;
import net.bytebuddy.asm.Advice.OnMethodEnter;
import net.bytebuddy.asm.Advice.OnMethodExit;
import net.bytebuddy.asm.Advice.Origin;
import net.bytebuddy.asm.Advice.Return;
import net.bytebuddy.asm.Advice.This;
import net.bytebuddy.asm.Advice.Thrown;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;


public class BBAdvice {
  @OnMethodEnter
  public static void onEnter(@This(optional = true) Object self, @Origin Method method,
      @AllArguments Object[] args) throws Throwable {
    AppMapAppMethod annot = method.getAnnotation(AppMapAppMethod.class);
    int[] values = annot.value();
    EventTemplateRegistry etr = EventTemplateRegistry.get();
    int callTemplateOrdinal = values[MethodEvent.METHOD_INVOCATION.getIndex()];

    ThreadLock.current().enter();

    if (ThreadLock.current().lock()) {
      MethodCall.handle(etr.buildCallEvent(callTemplateOrdinal), self, args);
      ThreadLock.current().unlock();
    } ;
  }

  @OnMethodExit(onThrowable = Throwable.class)
  public static void onExit(@This(optional = true) Object self, @Origin Method method,
      @AllArguments Object[] args, @Return(typing = Typing.DYNAMIC) Object ret,
      @Thrown Throwable exc) throws Throwable {
    try {
      if (exc == null) {
        handleReturn(self, method, args, ret);
      } else {
        handleExc(self, method, args, exc);
      }
    } finally {
      ThreadLock.current().exit();
    }
  }

  public static void handleReturn(Object self, Method method, Object[] args, Object ret)
      throws Throwable {
    EventTemplateRegistry etr = EventTemplateRegistry.get();
    AppMapAppMethod annot = method.getAnnotation(AppMapAppMethod.class);
    int[] values = annot.value();
    int returnTemplateOrdinal = values[MethodEvent.METHOD_RETURN.getIndex()];

    if (ThreadLock.current().lock()) {
      MethodReturn.handle(etr.buildReturnEvent(returnTemplateOrdinal), self, ret, args);
      ThreadLock.current().unlock();
    }
  }

  public static void handleExc(Object self, Method method,
      Object[] args, Throwable exc) throws Throwable {
    AppMapAppMethod annot = method.getAnnotation(AppMapAppMethod.class);
    int[] values = annot.value();
    EventTemplateRegistry etr = EventTemplateRegistry.get();
    int excTemplateOrdinal = values[MethodEvent.METHOD_EXCEPTION.getIndex()];

    if (ThreadLock.current().lock()) {
      MethodException.handle(etr.buildReturnEvent(excTemplateOrdinal), self, exc, args);
      ThreadLock.current().unlock();
    }
    throw exc;
  }
}
