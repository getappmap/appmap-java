package com.appland.appmap.transform;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.EventProcessorType;
import com.appland.appmap.record.EventFactory;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.stream.Collectors;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

class SelectiveClassFileTransformer {
  private static EventFactory eventFactory = EventFactory.get();

  public SelectiveClassFileTransformer() {
    super();
  }

  public byte[] transform(ClassLoader loader, CtClass classType)
      throws CannotCompileException, IOException {

    CtBehavior[] behaviors = classType.getDeclaredMethods();
    for (CtBehavior behavior : behaviors) {
      if (this.canTransformBehavior(behavior) == false) {
        continue;
      }

      Integer behaviorOrdinal = SelectiveClassFileTransformer.eventFactory.register(behavior);
      Event eventTemplate = SelectiveClassFileTransformer.eventFactory.getTemplate(behaviorOrdinal);

      this.transformBehavior(behavior, behaviorOrdinal, eventTemplate);
    }

    return classType.toBytecode();
  }

  public Boolean canTransformClass(CtClass classType) {
    return true;
  }

  public Boolean canTransformBehavior(CtBehavior behavior) {
    return true;
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
                                      EventProcessorType eventProcessor) {
    return String.format("%s(new Integer(%d), %s.%s, %s($_));",
        "com.appland.appmap.process.MethodCallback.onMethodReturn",
        behaviorOrdinal,
        "com.appland.appmap.process.EventProcessorType",
        eventProcessor,
        "com.appland.appmap.process.MethodCallback.boxValue");
  }

  public void transformBehavior(CtBehavior behavior, Integer behaviorOrdinal, Event eventTemplate)
      throws CannotCompileException {
    final EventProcessorType eventProcessor = this.getProcessorType(behavior);

    behavior.insertBefore(
        SelectiveClassFileTransformer.buildPreHook(behavior,
                                                   behaviorOrdinal,
                                                   eventTemplate,
                                                   eventProcessor));

    behavior.insertAfter(
        SelectiveClassFileTransformer.buildPostHook(behavior,
                                                    behaviorOrdinal,
                                                    eventTemplate,
                                                    eventProcessor));

    if (System.getenv("APPMAP_DEBUG") != null) {
      System.out.printf("Hooking %s.%s with %s\n",
          behavior.getDeclaringClass().getName(),
          behavior.getName(),
          eventProcessor);
    }
  }

  public EventProcessorType getProcessorType(CtBehavior behavior) {
    return EventProcessorType.Null;
  }
}
