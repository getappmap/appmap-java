package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import javassist.CtBehavior;
import javassist.CtClass;

public class HttpClassTransformer extends SelectiveClassFileTransformer {
  private static final ClassProcessorInfo httpClasses = new ClassProcessorInfo()
      .addClass("javax.servlet.http.HttpServlet",
        new BehaviorInfo("service")
          .addParam("javax.servlet.http.HttpServletRequest")
          .addParam("javax.servlet.http.HttpServletResponse")
          .processedBy(EventProcessorType.Http_Tomcat));

  public HttpClassTransformer() {
    super();
  }

  @Override
  public Boolean canTransformClass(CtClass classType) {
    return httpClasses.isKnownClass(classType);
  }

  @Override
  public Boolean canTransformBehavior(CtBehavior behavior) {
    return httpClasses.isKnownClassBehavior(behavior);
  }

  @Override
  public EventProcessorType getProcessorType(CtBehavior behavior) {
    return EventProcessorType.Http_Tomcat;
  }
}
