package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javassist.CtBehavior;
import javassist.CtClass;

class ClassProcessorInfo {
  private HashMap<String, List<BehaviorInfo>> classInfo = new HashMap<>();

  public ClassProcessorInfo add(String className, BehaviorInfo... behaviors) {
    classInfo.put(className, Arrays.asList(behaviors));
    return this;
  }

  public EventProcessorType getEventProcessorType(CtBehavior behavior) {
    final String className = behavior.getDeclaringClass().getName();
    final List<BehaviorInfo> behaviors = classInfo.get(className);
    if (behaviors == null) {
      return null;
    }

    for (BehaviorInfo info : behaviors) {
      if (info.describesBehavior(behavior)) {
        return info.getEventProcessorType();
      }
    }

    return null;
  }

  public Boolean containsClass(CtClass classType) {
    final String className = classType.getName();
    return this.classInfo.containsKey(className);
  }

  public Boolean containsBehavior(CtBehavior behavior) {
    return this.getEventProcessorType(behavior) != null;
  }
}