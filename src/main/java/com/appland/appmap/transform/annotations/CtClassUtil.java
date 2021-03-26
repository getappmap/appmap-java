package com.appland.appmap.transform.annotations;

import com.appland.appmap.config.Properties;
import com.appland.appmap.util.Logger;

import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Utility methods for working with CtClass and related types.
 */
class CtClassUtil {
  /**
   * Checks whether or not two types are equal or related via class hierarchy or interface
   * implementation.
   * @param candidateChildClass The child class
   * @param parentClassName The name of the parent class
   * @return {@code true} if the parent class is a super class of the child or equal to the child.
   *         Otherwise, {@code false}.
   */
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
      Logger.println("could not resolve class hierarchy");
      Logger.println(e);
    }

    return false;
  }
}
