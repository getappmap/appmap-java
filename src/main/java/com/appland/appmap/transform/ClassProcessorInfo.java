package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

class ClassProcessorInfo {
  enum DefinitionType { Class, Interface }

  private HashMap<String, List<BehaviorInfo>>    classes = new HashMap<>();
  private HashMap<String, List<BehaviorInfo>> interfaces = new HashMap<>();

  public ClassProcessorInfo addClass(String className, BehaviorInfo... behaviors) {
    classes.put(className, Arrays.asList(behaviors));
    return this;
  }

  public ClassProcessorInfo addInterface(String interfaceName, BehaviorInfo... behaviors) {
    interfaces.put(interfaceName, Arrays.asList(behaviors));
    return this;
  }

  public EventProcessorType getEventProcessorType(CtBehavior behavior) {
    final String className = behavior.getDeclaringClass().getName();
    final List<BehaviorInfo> behaviors = classes.get(className);
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

  public Boolean inheritsKnownInterface(CtClass classType) {
    try {
      final CtClass[] interfaces = classType.getInterfaces();

      for (String registeredClassName : this.interfaces.keySet()) {
        for (CtClass superType : interfaces) {
          final String superTypeClassName = superType.getName();
          if (registeredClassName.equals(superTypeClassName)) {
            return true;
          }
        }
      }
    } catch (NotFoundException e) {
      // fall through
    }

    return false;
  }

  public Boolean isKnownClass(CtClass classType) {
    final String className = classType.getName();
    return this.classes.containsKey(className);
  }

  public Boolean isKnownClassBehavior(CtBehavior behavior) {
    return this.getEventProcessorType(behavior) != null;
  }

  private List<BehaviorInfo> getInterfaceBehaviors(CtClass classType) {
    try {
      final CtClass[] superTypes = classType.getInterfaces();

      for (Map.Entry<String, List<BehaviorInfo>> interfaceEntry : this.interfaces.entrySet()) {
        for (CtClass superType : superTypes) {
          final String superTypeName = superType.getName();
          if (interfaceEntry.getKey().equals(superTypeName)) {
            return (List<BehaviorInfo>) interfaceEntry.getValue();
          }
        }
      }
    } catch (NotFoundException e) {
      // fall through
    }

    return null;
  }

  public Boolean isKnownInterfaceBehavior(CtBehavior behavior) {
    final CtClass classType = behavior.getDeclaringClass();
    List<BehaviorInfo> behaviorInfos = this.getInterfaceBehaviors(classType);

    if (behaviorInfos == null) {
      return false;
    }

    for (BehaviorInfo behaviorInfo : behaviorInfos) {
      if (behaviorInfo.describesBehavior(behavior)) {
        return true;
      }
    }

    return false;
  }
}