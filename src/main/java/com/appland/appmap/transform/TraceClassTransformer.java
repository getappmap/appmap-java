package com.appland.appmap.transform;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.EventProcessorType;
import com.appland.appmap.process.MethodCallback;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

public class TraceClassTransformer extends SelectiveClassFileTransformer {
  private AppMapConfig config;

  public TraceClassTransformer(AppMapConfig config) {
    this.config = config;
  }

  private Boolean shouldHookClass(CtClass classType) {
    if (this.config == null || this.config.packages == null) {
      return false;
    }

    final String className = classType.getName();
    for (AppMapPackage packageConfig : this.config.packages) {
      if (className.startsWith(packageConfig.path) == false) {
        continue;
      }

      if (packageConfig.exclude != null) {
        for (String exclusion : packageConfig.exclude) {
          if (className.startsWith(exclusion)) {
            return false;
          }
        }
      }

      return true;
    }
    return false;
  }

  @Override
  public Boolean canTransformClass(CtClass classType) {
    return this.shouldHookClass(classType);
  }

  @Override
  public Boolean canTransformBehavior(CtBehavior behavior) {
    if ((behavior.getModifiers() & Modifier.PUBLIC) == 0) {
      return false;
    }

    if (behavior.getMethodInfo().getLineNumber(0) == -1) {
      // auto generated code
      return false;
    }

    if (behavior.getName().contains("$")) {
      return false;
    }

    return true;
  }

  @Override
  public EventProcessorType getProcessorType(CtBehavior behavior) {
    return EventProcessorType.PassThrough;
  }

  @Override
  public void transformBehavior(CtBehavior behavior, Integer behaviorOrdinal, Event eventTemplate)
      throws CannotCompileException {
    super.transformBehavior(behavior, behaviorOrdinal, eventTemplate);
    MethodCallback.onBehaviorTransformed(behavior);
  }
}
