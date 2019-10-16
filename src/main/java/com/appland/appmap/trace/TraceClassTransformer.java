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
import javassist.bytecode.Bytecode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.stream.Collectors;

import com.appland.appmap.trace.Agent;
import com.appland.appmap.trace.TraceEventFactory;
import com.appland.appmap.trace.TraceListenerRecord;

import com.appland.appmap.output.v1.Event;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class TraceClassTransformer implements ClassFileTransformer {
  public TraceClassTransformer() {
    super();
  }

  private static String getPreHook(CtBehavior behavior, Event event, Integer methodId) {
    String paramArray = "new Object[0]";
    if (event.parameters.size() > 0) {
      paramArray = event.parameters
          .stream()
          .map(p -> String.format("com.appland.appmap.trace.Agent.box(%s)", p.name))
          .collect(Collectors.joining(", ", "new Object[]{ ", " }"));
    }

    Boolean isStatic = (behavior.getModifiers() & Modifier.STATIC) != 0;
    //  "__$APPMAP$THREAD_LOCK = com.appland.appmap.trace.Agent.onCall(new Integer(%d), %s, %s);",
    return String.format(
        "com.appland.appmap.trace.Agent.onCall(new Integer(%d), %s, %s);",
        methodId,
        isStatic ? "null" : "this",
        paramArray);
  }

  private static String getPostHook(CtBehavior behavior, Integer methodId) {
    return String.format(
        "com.appland.appmap.trace.Agent.onReturn(new Integer(%d), $_);",
        methodId);
  }

  // private static void testTransform(CtBehavior behavior, Event event) {
  //   CodeAttribute codeAttribute = behavior.getMethodInfo().getCodeAttribute();
  //   CodeIterator iter = codeAttribute.iterator();
  //   Bytecode preHook = new Bytecode();


  //   if (event.isStatic == false) {
  //     preHook.addAload(0);
  //   }

  //   for(CodeIterator iter = codeAttribute.iterator(); iter.hasNext();) {
  //     int i = iter.next();
  //     int opCode = iter.byteAt(i);
  //     // if (opCode == Bytecode.ARETURN ||
  //     //     opCode == Bytecode.DRETURN ||
  //     //     opCode == Bytecode.FRETURN ||
  //     //     opCode == Bytecode.IRETURN ||
  //     //     opCode == Bytecode.LRETURN ||
  //     //     opCode == Bytecode.RETURN) {

  //     // }
  //   }
  // }

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

      TraceEventFactory eventFactory = TraceListenerRecord.getEventFactory();
      CtBehavior[] behaviors = ctClass.getDeclaredMethods();
      for (CtBehavior behavior : behaviors) {
        if (TraceUtil.isRelevant(behavior) == false) {
          continue;
        }

        if (TraceUtil.isDebugMode()) {
          System.out.printf("\n\nNew hook -> %s.%s\n\n", ctClass.getName(), behavior.getName());
        }

        Integer methodId = eventFactory.register(behavior);
        // testTransform(behavior, eventFactory.getTemplate(methodId));

        // try {
        // behavior.addLocalVariable("__$APPMAP$THREAD_LOCK", classPool.get("java.lang.Long"));
        behavior.insertBefore(TraceClassTransformer.getPreHook(behavior, eventFactory.getTemplate(methodId), methodId));
        behavior.insertAfter(TraceClassTransformer.getPostHook(behavior, methodId));
        // } catch (NotFoundException e) {
        //   if (TraceUtil.isDebugMode()) {
        //     System.err.printf("error: %s\n", e.getMessage());
        //   }
        // }
        
 
        // behavior.instrument(
        //     new ExprEditor() {
        //       public void edit(MethodCall m) throws CannotCompileException {
        //         String code = String.format("{ %s $_ = $proceed($$); %s }",
        //             TraceClassTransformer.getPreHook(behavior, eventFactory.getTemplate(methodId), methodId),
        //             TraceClassTransformer.getPostHook(behavior, methodId));
        //         // System.out.println(code);
        //         m.replace(code);
        //       }
        //   });

        if (TraceUtil.isDebugMode()) {
          System.out.print("\nBytecode:\n");
          InstructionPrinter.print((CtMethod) behavior, System.out);
        }
      }

      Agent.get().onClassLoad(ctClass);

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