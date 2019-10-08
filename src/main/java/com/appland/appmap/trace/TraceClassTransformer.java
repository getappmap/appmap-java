package com.appland.appmap.trace;

import javassist.tools.Callback;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtBehavior;
import javassist.CtNewMethod;
import javassist.CtConstructor;
import javassist.CannotCompileException;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.InstructionPrinter;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import com.appland.appmap.trace.Agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class TraceClassTransformer implements ClassFileTransformer {
  public TraceClassTransformer() {
    super();
  }

  private static String getPreHook(CtBehavior behavior, Integer methodOrdinal) {
    Boolean isStatic = (behavior.getModifiers() & Modifier.STATIC) != 0;

    return String.format(
        "com.appland.appmap.trace.Agent.onCall(%s.class, %d, %s, $args);",
        behavior.getDeclaringClass().getName(),
        methodOrdinal,
        isStatic ? "null" : "$0");
  }

  private static String getPostHook(CtBehavior behavior, Integer methodOrdinal) {
    return String.format(
        "com.appland.appmap.trace.Agent.onReturn(%s.class, %d, com.appland.appmap.trace.Agent.box($_));",
        behavior.getDeclaringClass().getName(),
        methodOrdinal);
  }

  public byte[] transform(ClassLoader loader, String className, Class redefiningClass, ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
    try {
      ClassPool classPool = new ClassPool();
      classPool.appendClassPath(new LoaderClassPath(loader));

      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(bytes));

      // don't hook interfaces
      if (ctClass.isInterface()) {
        return bytes;
      }

      if (!Agent.shouldHook(ctClass.getName())) {
        return bytes;
      }

      CtBehavior[] behaviors = ctClass.getDeclaredMethods();
      Integer i = -1;
      for (CtBehavior behavior : behaviors) {
        if (behavior.getName().contains("$")) {
          continue;
        }

        // hooking toString could cause a stack overflow
        if (behavior.getName().equals("toString")) {
          continue;
        }

        // I'm not confident in this methodology of counting the method ordinal.
        // It also seems finicky against code modified by cglib.
        // -db
        final Integer methodOrdinal = i++;

        if ((behavior.getModifiers() & Modifier.PUBLIC) == 0) {
          continue;
        }

        behavior.instrument(
            new ExprEditor() {
              public void edit(MethodCall m) throws CannotCompileException {
                m.replace(
                    String.format("{{%s} {$_ = $proceed($$);} {%s}}",
                        TraceClassTransformer.getPreHook(behavior, methodOrdinal),
                        TraceClassTransformer.getPostHook(behavior, methodOrdinal)));
              }
          });

        if (TraceUtil.isDebugMode()) {
          System.out.println(
              String.format("Hooked %s.%s (%d)",
                  ctClass.getName(),
                  behavior.getName(),
                  methodOrdinal));
        }
      }

      return ctClass.toBytecode();
    } catch (IOException e) {
      // do nothing
    } catch (CannotCompileException e) {
      System.err.println(e.getMessage());
    }

    System.out.println("Transformer to Transform Class: " + className);
    return bytes;
  }
}