package com.appland.appmap.transform;

import com.appland.appmap.output.v1.CodeObject;
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
  private static final Boolean debug = System.getProperty("appmap.debug") != null;

  // TODO: Enable appmap.yml to build all these Hookable objects.
  private static final Hookable hooks = new Hookable(
      new HookableInterfaceName(ClassReference.create("javax", "servlet", "Filter"),
        new HookableMethodSignature("doFilter")
          .addParam("javax.servlet.ServletRequest")
          .addParam("javax.servlet.ServletResponse")
          .addParam("javax.servlet.FilterChain")
          .processedBy(EventProcessorType.ServletFilter)
      ),

      new HookableClassName(ClassReference.create("javax", "servlet", "http", "HttpServlet"),
        new HookableMethodSignature("service")
          .addParam(ClassReference.create("javax", "servlet", "http", "HttpServletRequest"))
          .addParam(ClassReference.create("javax", "servlet", "http", "HttpServletResponse"))
          .processedBy(EventProcessorType.HttpServlet)
      ),

      new HookableInterfaceName(ClassReference.create("java", "sql", "Connection"),
        new HookableMethodSignature("nativeSQL").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("prepareCall").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("prepareStatement").processedBy(EventProcessorType.SqlJdbc)
      ),

      new HookableInterfaceName(ClassReference.create("java", "sql", "Statement"),
        new HookableMethodSignature("addBatch").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("execute").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("executeQuery").processedBy(EventProcessorType.SqlJdbc),
        new HookableMethodSignature("executeUpdate").processedBy(EventProcessorType.SqlJdbc)
      ),

      new HookableAnnotated(ClassReference.create("org", "junit", "Test"))
        .processedBy(EventProcessorType.ToggleRecord),

      new HookableClassName(ClassReference.create("org", "apache", "lucene", "util", "LuceneTestCase"),
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
      CtClass ctClass = null;

      try {
        ctClass = classPool.makeClass(new ByteArrayInputStream(bytes));
      } catch (RuntimeException e) {
        // The class is frozen
        // We can defrost it and apply our changes, though in practice I've observed this to be
        // unstable. Particularly, exceptions thrown from the Groovy runtime due to missing methods.
        // There's likely a way to do this safely, but further investigation is needed.
        //
        // ctClass = classPool.get(className.replace('/', '.'));
        // ctClass.defrost();
        //
        // For now, just skip this class
        return bytes;
      }

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
      e.printStackTrace(System.err);
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
          .map(p -> String.format("com.appland.appmap.process.BehaviorEntrypoints.boxValue(%s)", p.name))
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
        "com.appland.appmap.process.BehaviorEntrypoints.onEnter",
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
                                      EventProcessorType processorType,
                                      String returnValue) {
    return String.format("%s(new Integer(%d), %s.%s, %s(%s));",
        "com.appland.appmap.process.BehaviorEntrypoints.onExit",
        behaviorOrdinal,
        "com.appland.appmap.process.EventProcessorType",
        processorType,
        "com.appland.appmap.process.BehaviorEntrypoints.boxValue",
        returnValue);
  }

  public void transformBehavior(CtBehavior behavior,
                                EventProcessorType processorType,
                                Integer behaviorOrdinal,
                                Event eventTemplate) 
                                throws CannotCompileException, NotFoundException {
    behavior.insertBefore(
        buildPreHook(behavior, behaviorOrdinal, eventTemplate, processorType)
    );
    behavior.insertAfter(
        buildPostHook(behavior, behaviorOrdinal, eventTemplate, processorType, "$_")
    );
    behavior.addCatch(
        String.format("%s throw e;",
            buildPostHook(behavior, behaviorOrdinal, eventTemplate, processorType, "null")),
        ClassPool.getDefault().get("java.lang.Throwable"),
        "e"
    );

    if (debug) {
      System.err.printf("Hooking %s.%s with %s\n",
          behavior.getDeclaringClass().getName(),
          behavior.getName(),
          processorType);
    }

    // TODO
    // record the code behavior
    // CodeObject rootObject = CodeObject.createTree(behavior);
    // RuntimeRecorder.get().recordCodeObject(rootObject);
  }
}
