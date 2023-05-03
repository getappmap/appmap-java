package com.appland.appmap.transform.annotations;

import com.appland.appmap.config.Properties;
import com.appland.appmap.util.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Utility methods for working with CtClass and related types.
 */
class CtClassUtil {
  /**
   * Checks whether or not two types are equal or related via class hierarchy or interface
   * implementation.
   *
   * @param candidateChildClass The child class
   * @param parentClassName     The name of the parent class
   * @return {@code true} if the parent class is a super class of the child or equal to the child.
   * Otherwise, {@code false}.
   */
  public static Boolean isChildOf(CtClass candidateChildClass, String parentClassName) {
    // System.err.println("candidateChildClass: " + candidateChildClass.getName() + ", parentClassName: " + parentClassName);

    if (candidateChildClass.getName().equals(parentClassName)) {
      return true;
    }

    CtClass[] interfaces = tryClass(candidateChildClass, "interfaces", candidateChildClass::getInterfaces);
    if ( interfaces != null ) {
      for (CtClass superType : interfaces) {
        //System.err.println("interface: " + superType.getName());
        if (superType.getName().equals(parentClassName)) {
          return true;
        } else {
          if (isChildOf(superType, parentClassName)) {
            return true;
          }
        }
      }
    }

    CtClass superClass = tryClass(candidateChildClass, "superclass", candidateChildClass::getSuperclass);
    while (superClass != null) {
      if (superClass.getName().equals(parentClassName)) {
        return true;
      }
      /*
      else if (isChildOf(superClass, parentClassName)) {
        return true;
      }
      */
      
      final CtClass cls = superClass;
      superClass = tryClass(cls, "superclass", cls::getSuperclass);
    }

    return false;
  }

  private static <V> V tryClass(CtClass cls, String member, ClassAccessor<V> accessor) {
    try {
      return accessor.navigate();
    } catch (NotFoundException e) {
      if (Properties.DebugHooks) {
        Logger.printf("NotFoundException resolving %s of class %s: %s\n", member, cls.getName(), e.getMessage());
      }
      return null;
    }
  }

  interface ClassAccessor<V> {
    V navigate() throws NotFoundException;
  }

  public static Boolean isChildOf(String childClassName, CtClass parentClass) {
    ClassPool cp = ClassPool.getDefault();
    try {
      CtClass childClass = cp.get(childClassName);
      return isChildOf(childClass, parentClass.getName());
    } catch (NotFoundException e) {
      Logger.println(e);
    }
    return false;
  }
}
