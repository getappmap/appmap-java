package com.appland.appmap.util;

import java.util.ArrayDeque;
import java.util.Deque;

import com.appland.appmap.Agent;
import javassist.ClassPool;
import javassist.LoaderClassPath;

public class AppMapClassPool {
  private static final ThreadLocal<Deque<ClassPool>> threadPoolQueue =
      ThreadLocal.withInitial(() -> new ArrayDeque<ClassPool>());

  public static ClassPool acquire(ClassLoader classLoader) {
    ClassPool ret = new ClassPool();
    ret.appendClassPath(new LoaderClassPath(classLoader));
    ret.appendClassPath(new LoaderClassPath(Agent.class.getClassLoader()));
    threadPoolQueue.get().push(ret);
    return ret;
  }

  public static ClassPool get() {
    ClassPool ret = threadPoolQueue.get().peek();
    if (ret == null) {
      throw new InternalError("not acquired");
    }
    return ret;
  }

  public static void release() {
    threadPoolQueue.get().pop();
  }
}
