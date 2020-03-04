package com.appland.appmap.transform.annotations;

import javassist.CtClass;
import javassist.NotFoundException;

class CtClassUtil {
  public static Boolean isChildOf(CtClass candidateChildClass, String parentClassName) {
    try {
      if (candidateChildClass.getName().equals(parentClassName)) {
        return true;
      }

      for (CtClass superType : candidateChildClass.getInterfaces()) {
        if (superType.getName().equals(parentClassName)) {
          return true;
        } else {
          if (isChildOf(superType, parentClassName)) {
            return true;
          }
        }
      }

      CtClass superClass = candidateChildClass.getSuperclass();
      while (superClass != null) {
        if (superClass.getName().equals(parentClassName)) {
          return true;
        }
        superClass = superClass.getSuperclass();
      }
    } catch (NotFoundException e) {
      System.err.println("AppMap: could not resolve class hierarchy");
      System.err.println(e.getMessage());
    }

    return false;
  }
}