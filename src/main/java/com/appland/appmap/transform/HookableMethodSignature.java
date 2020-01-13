package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.util.ArrayList;
import java.util.HashMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class HookableMethodSignature extends Hookable {
  private String methodName;
  private ArrayList<String> paramTypes = new ArrayList<String>();
  private String returnType;

  public HookableMethodSignature(String methodName) {
    this.methodName = methodName;
  }

  public HookableMethodSignature addParam(String typeName) {
    this.paramTypes.add(typeName);
    return this;
  }

  public HookableMethodSignature returns(String typeName) {
    this.returnType = typeName;
    return this;
  }

  @Override
  protected Boolean match(CtBehavior behavior) {
    if (!behavior.getName().equals(this.methodName)) {
      return false;
    }

    if (!this.paramTypes.isEmpty()) {
      try {
        CtClass[] paramTypes = behavior.getParameterTypes();
        if (this.paramTypes.size() != paramTypes.length) {
          return false;
        }

        for (int i = 0; i < paramTypes.length; ++i) {
          final String paramName = this.paramTypes.get(i);
          if (!paramName.equals(paramTypes[i].getName())) {
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

      Boolean returnTypeMatches = false;
      try {
        CtMethod method = (CtMethod) behavior;
        returnTypeMatches = this.returnType.equals(method.getReturnType().getName());
      } catch (NotFoundException e) {
        // fall through
      }
      return returnTypeMatches;
    }

    return true;
  }
}
