package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import javassist.CtBehavior;
import javassist.CtClass;

public class ServletFilterClassTransformer extends SelectiveClassFileTransformer {
  private static final ClassProcessorInfo filterDefinitions = new ClassProcessorInfo()
      .addInterface("javax.servlet.Filter",
        new BehaviorInfo("doFilter")
          .addParam("javax.servlet.ServletRequest")
          .addParam("javax.servlet.ServletResponse")
          .addParam("javax.servlet.FilterChain")
          .processedBy(EventProcessorType.ServletFilter));

  public ServletFilterClassTransformer() {
    super();
  }

  @Override
  public Boolean canTransformClass(CtClass classType) {
    return filterDefinitions.inheritsKnownInterface(classType);
  }

  @Override
  public Boolean canTransformBehavior(CtBehavior behavior) {
    return filterDefinitions.isKnownInterfaceBehavior(behavior);
  }

  @Override
  public EventProcessorType getProcessorType(CtBehavior behavior) {
    return EventProcessorType.ServletFilter;
  }
}
