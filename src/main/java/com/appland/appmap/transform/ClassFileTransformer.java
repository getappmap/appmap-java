package com.appland.appmap.transform;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.EventProcessorType;
import com.appland.appmap.record.EventFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.stream.Collectors;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

/**
 * ClassFileTransformer transforms classes to send callback notifications to com.appland.appmap.process.
 * Only classes and methods which match the <code>appmap.yml</code> are transformed in this way.
 */
public class ClassFileTransformer implements java.lang.instrument.ClassFileTransformer {
  private static final EventFactory eventFactory = EventFactory.get();

  // TODO: Enable appmap.yml to build all these Hookable objects.
  private static final Hookable hooks = new Hookable(
      new HookableInterfaceName("javax.servlet.Filter",
        new HookableMethodSignature("doFilter")
          .addParam("javax.servlet.ServletRequest")
          .addParam("javax.servlet.ServletResponse")
          .addParam("javax.servlet.FilterChain")
          .processedBy(EventProcessorType.ServletFilter)
      ),

      new HookableClassName("javax.servlet.http.HttpServlet",
        new HookableMethodSignature("service")
          .addParam("javax.servlet.http.HttpServletRequest")
          .addParam("javax.servlet.http.HttpServletResponse")
          .processedBy(EventProcessorType.HttpServlet)
      ),

      new HookableInterfaceName("java.sql.Connection",
        new HookableMethodSignature("nativeSQL").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("prepareCall").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("prepareStatement").processedBy(EventProcessorType.SqlJdbc)
      ),

      new HookableInterfaceName("java.sql.Statement",
        new HookableMethodSignature("addBatch").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("execute").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("executeQuery").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("executeUpdate").processedBy(EventProcessorType.SqlJdbc)
      ),

      new HookableAnnotated("org.junit.Test").processedBy(EventProcessorType.ToggleRecord),
      new HookableClassName("org.elasticsearch.test.ESTestCase",
        new HookableAllMethods().processedBy(EventProcessorType.ToggleRecord)
      ),
      new HookableClassName("org.elasticsearch.test.ESIntegTestCase",
        new HookableAllMethods().processedBy(EventProcessorType.ToggleRecord)
      ),

      new HookableConfigPath().processedBy(EventProcessorType.PassThrough)
  );

  public ClassFileTransformer() {
    super();
  }

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class redefiningClass,
                          ProtectionDomain domain,
                          byte[] bytes) throws IllegalClassFormatException {
    ClassPool classPool = ClassPool.getDefault();
    classPool.appendClassPath(new LoaderClassPath(loader));

    try {
      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(bytes));
      if (ctClass.isInterface()) {
        return bytes;
      }

      for (BehaviorProcessorPair pair : hooks.getBehaviors(ctClass)) {
        final CtBehavior behavior = pair.getBehavior();
        Integer behaviorOrdinal = eventFactory.register(behavior);
        Event eventTemplate = eventFactory.getTemplate(behaviorOrdinal);

        transformBehavior(behavior,
                          pair.getProcessorType(),
                          behaviorOrdinal,
                          eventTemplate);
      }

      return ctClass.toBytecode();
    } catch (Exception e) {
      // Don't allow this exception to propagate out of this method, because it will be swallowed
      // by sun.instrument.TransformerManager.
      System.err.println("An error occurred transforming class " + className);
      System.err.println(e.getClass() + ": " + e.getMessage());
    }

    return bytes;
  }


  private static String buildPreHook(CtBehavior behavior,
                                     Integer behaviorOrdinal,
                                     Event eventTemplate,
                                     EventProcessorType eventProcessor) {
    String paramArray = "new Object[0]";
    if (eventTemplate.parameters.size() > 0) {
      paramArray = eventTemplate.parameters
          .stream()
          .map(p -> String.format("com.appland.appmap.process.MethodCallback.boxValue(%s)", p.name))
          .collect(Collectors.joining(", ", "new Object[]{ ", " }"));
    }

    Boolean isStatic = (behavior.getModifiers() & Modifier.STATIC) != 0;
    Boolean returnsVoid = true;
    Boolean unknownReturnType = false;
    if (behavior instanceof CtMethod) {
      try {
        CtMethod method = (CtMethod) behavior;
        returnsVoid = method.getReturnType() == CtClass.voidType;
      } catch (NotFoundException e) {
        unknownReturnType = true;
      }
    }

    String returnStatement = "";
    if (unknownReturnType == false) {
      returnStatement = returnsVoid ? "return;" : "return ($r) new Object();";
    }

    return String.format("if (%s(new Integer(%d), %s.%s, %s, %s) == false) { %s }",
        "com.appland.appmap.process.MethodCallback.onMethodInvocation",
        behaviorOrdinal,
        "com.appland.appmap.process.EventProcessorType",
        eventProcessor,
        isStatic ? "null" : "this",
        paramArray,
        returnStatement);
  }

  private static String buildPostHook(CtBehavior behavior,
                                      Integer behaviorOrdinal,
                                      Event eventTemplate,
                                      EventProcessorType processorType) {
    return String.format("%s(new Integer(%d), %s.%s, %s($_));",
        "com.appland.appmap.process.MethodCallback.onMethodReturn",
        behaviorOrdinal,
        "com.appland.appmap.process.EventProcessorType",
        processorType,
        "com.appland.appmap.process.MethodCallback.boxValue");
  }

  public void transformBehavior(CtBehavior behavior,
                                EventProcessorType processorType,
                                Integer behaviorOrdinal,
                                Event eventTemplate) throws CannotCompileException {
    behavior.insertBefore(buildPreHook(behavior, behaviorOrdinal, eventTemplate, processorType));
    behavior.insertAfter(buildPostHook(behavior, behaviorOrdinal, eventTemplate, processorType));

    if (System.getenv("APPMAP_DEBUG") != null) {
      System.err.printf("Hooking %s.%s with %s\n",
          behavior.getDeclaringClass().getName(),
          behavior.getName(),
          processorType);
    }
  }
}
