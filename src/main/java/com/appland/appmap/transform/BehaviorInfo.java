package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.util.ArrayList;
import java.util.HashMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

class BehaviorInfo {
  private String name;
  private ArrayList<String> paramTypes = new ArrayList<>();
  private String returnType;
  private EventProcessorType processorType = EventProcessorType.Null;

  public BehaviorInfo(String name) {
    this.name = name;
  }

  public BehaviorInfo addParam(String typeName) {
    this.paramTypes.add(typeName);
    return this;
  }

  public BehaviorInfo returns(String typeName) {
    this.returnType = typeName;
    return this;
  }

  public BehaviorInfo processedBy(EventProcessorType eventProcessor) {
    this.processorType = eventProcessor;
    return this;
  }

  public Boolean describesBehavior(CtBehavior behavior) {
    if (this.name.equals(behavior.getName()) == false) {
      return false;
    }

    if (this.paramTypes.isEmpty() == false) {
      try {
        CtClass[] paramTypes = behavior.getParameterTypes();
        if (this.paramTypes.size() != paramTypes.length) {
          return false;
        }

        for (int i = 0; i < paramTypes.length; ++i) {
          final String paramName = this.paramTypes.get(i);
          if (paramName.equals(paramTypes[i].getName()) == false) {
            return false;
          }
        }
      } catch (NotFoundException e) {
        return false;
      }
    }

    if (this.returnType != null) {
      if (!(behavior instanceof CtMethod)) {
        // it's a constructor
        return false;
      }

      CtMethod method = (CtMethod) behavior;

      Boolean returnTypeMatches = false;
      try {
        returnTypeMatches = this.returnType.equals(method.getReturnType().getName());
      } catch (NotFoundException e) {
        // fall through
      }
      return returnTypeMatches;
    }

    return true;
  }

  public EventProcessorType getEventProcessorType() {
    return this.processorType;
  }
}