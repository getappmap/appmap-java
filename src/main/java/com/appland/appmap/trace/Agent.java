package com.appland.appmap.trace;

import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import java.util.Set;
import java.util.HashMap;
import java.util.List;

import com.appland.appmap.output.IAppMapSerializer;
import com.appland.appmap.output.v1.AppMapSerializer;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtBehavior;
import javassist.CtNewMethod;
import javassist.CtConstructor;
import javassist.CannotCompileException;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.ReflectionUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;

public class Agent extends TracePublisher implements ClassFileTransformer {
  private static Agent singleton = new Agent();
  private static ClassPool classPool;
  private static AppMapConfig config;
  private static ThreadLock threadLock = new ThreadLock();
  private TraceClassTransformer transformer = new TraceClassTransformer();
  private HashMap<String, ClassFileTransformer> transforms = new HashMap<String, ClassFileTransformer>();

  private Agent() {
    if (TraceUtil.isDebugMode()) {
      addListener(new TraceListenerDebug());
      CtClass.debugDump = "./appmap/debug";
    }
  }

  public static Agent get() {
    return singleton;
  }

  public Agent config(AppMapConfig config) {
    this.config = config;
    return this;
  }

  public Agent addListener(ITraceListener listener) {
    this.addEventListener(listener);
    return this;
  }

  public static Boolean shouldHook(String className) {
    if (config == null) {
      return false;
    }

    Boolean matched = false;
    for (AppMapPackage configPackage : config.packages) {
      if (className.startsWith(configPackage.path)) {
        for (String exclusion : configPackage.exclude) {
          if (className.startsWith(exclusion)) {
            return false;
          }
        }
        matched = true;
      }
    }

    return matched;
  }

  private static void hookClassLoaders(Set<Class<? extends URLClassLoader>> classLoaderTypes) {
    for (Class<? extends URLClassLoader> classLoaderType : classLoaderTypes) {
      try {
        System.out.println("###############");
        System.out.println(String.format("Hooking class loader: %s", classLoaderType.getName()));
        System.out.println("###############");

        CtClass classLoader = classPool.get(classLoaderType.getName());
        CtMethod[] methods = classLoader.getDeclaredMethods();
        for (CtMethod method : methods) {
          if (method.getName() != "loadClass") {
            continue;
          }

          method.insertBefore("Agent.onLoadClass(name);");
        }
        classLoader.toClass();
      } catch (NotFoundException e) {
        System.err.println(e.getMessage());
      } catch (CannotCompileException e) {
        System.err.println(e.getMessage());
      }
    }
  }

  public static void onLoadClass(String className) {
    System.out.println(String.format("loading class %s", className));
  }

  private static boolean isExcluded(String className, AppMapPackage packageConfig) {
    if (packageConfig.exclude == null) {
      return false;
    }

    for (String exclusion : packageConfig.exclude) {
      if (className.startsWith(exclusion)) {
        return true;
      }
    }
    return false;
  }

  private static void hookClasses(Set<String> classNames, AppMapPackage packageConfig) {
    for (String className : classNames) {
      if (isExcluded(className, packageConfig)) {
        continue;
      }

      hookClass(className);
    }
  }

  public TraceClassTransformer getClassTransformer() {
    return this.transformer;
  }

  public static void onCall(Integer methodId, Object selfValue, Object[] params) {
    // Long threadId = Agent.threadLock.getLock();
    // if (threadId >= 0) {
    //   singleton.onMethodInvoked(methodId, selfValue, params);
    // }
    // return threadId;
    if (Agent.threadLock.tryLock()) {
      singleton.onMethodInvoked(methodId, selfValue, params);
      Agent.threadLock.releaseLock();
    }
  }

  public static void onReturn(Integer methodId, byte returnValue) {
    Agent.onReturn(methodId, new Byte(returnValue));
  }

  public static void onReturn(Integer methodId, char returnValue) {
    Agent.onReturn(methodId, new Character(returnValue));
  }

  public static void onReturn(Integer methodId, short returnValue) {
    Agent.onReturn(methodId, new Short(returnValue));
  }

  public static void onReturn(Integer methodId, long returnValue) {
    Agent.onReturn(methodId, new Long(returnValue));
  }

  public static void onReturn(Integer methodId, float returnValue) {
    Agent.onReturn(methodId, new Float(returnValue));
  }

  public static void onReturn(Integer methodId, double returnValue) {
    Agent.onReturn(methodId, new Double(returnValue));
  }

  public static void onReturn(Integer methodId, int returnValue) {
    Agent.onReturn(methodId, new Integer(returnValue));
  }

  public static void onReturn(Integer methodId, boolean returnValue) {
    Agent.onReturn(methodId, new Boolean(returnValue));
  }

  public static void onReturn(Integer methodId, Object returnValue) {
    if (Agent.threadLock.tryLock()) {
      singleton.onMethodReturned(methodId, returnValue);
      Agent.threadLock.releaseLock();
    }
    // Agent.threadLock.releaseLock(threadId);
  }

  private static void hookClass(String className) {
    try {
      CtClass ctClass = classPool.get(className);
      if (ctClass.isInterface()) {
        return;
      }

      CtBehavior[] behaviors = ctClass.getDeclaredMethods();
      for (CtBehavior behavior : behaviors) {
        if ((behavior.getModifiers() & Modifier.PUBLIC) == 0) {
          continue;
        }

        String before = String.format(
            "Agent.onCall($class, \"%s\", $args, $sig);",
            behavior.getName());
        String after = String.format(
            "Agent.onReturn($class, \"%s\", Agent.box($_), $sig);",
            behavior.getName());

        // We can get source information by passing along a StackTraceElement
        // retrived from (new Throwable().getStackTrace[0]).
        //
        // https://docs.oracle.com/javase/7/docs/api/java/lang/StackTraceElement.html

        // We should also be wrapping the function in a catch-all try/catch block
        // to know when an unhandled exception causes the method to return prematurely

        try {
          behavior.insertBefore(before);
          behavior.insertAfter(after);

          if (TraceUtil.isDebugMode()) {
            System.out.println(
                String.format("Hooked %s.%s",
                    ctClass.getName(),
                    behavior.getName()));
          }
        } catch (CannotCompileException e) {
          System.err.println(String.format("  %s: %s", behavior.getName(), e.getMessage()));
        }
      }

      // Class loadedClass = ctClass.toClass();
      ctClass.writeFile();
      // singleton.onClassRegistered(loadedClass);
    } catch (NotFoundException e) {
      System.err.println(e.getMessage());
    } catch (CannotCompileException e) {
      System.err.println(e.getMessage());
    } catch (IOException e) {
      // do nothing
    }
  }

  public static Object box(byte a) { return Byte.valueOf(a); }
  public static Object box(char a) { return Character.valueOf(a); }
  public static Object box(short a) { return Short.valueOf(a); }
  public static Object box(long a) { return Long.valueOf(a); }
  public static Object box(float a) { return Float.valueOf(a); }
  public static Object box(double a) { return Double.valueOf(a); }
  public static Object box(int a) { return Integer.valueOf(a); }
  public static Object box(boolean a) { return Boolean.valueOf(a); }
  public static Object box(Object a) { return a; }
}