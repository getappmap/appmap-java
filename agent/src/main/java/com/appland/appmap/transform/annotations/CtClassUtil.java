package com.appland.appmap.transform.annotations;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.util.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Utility methods for working with CtClass and related types.
 */
class CtClassUtil {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

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
    logger.trace("candidateChildClass: {}, parentClassName: {}", candidateChildClass.getName(), parentClassName);

    if (candidateChildClass.getName().equals(parentClassName)) {
      return true;
    }

    CtClass[] interfaces = tryClass(candidateChildClass, "interfaces", candidateChildClass::getInterfaces);
    if ( interfaces != null ) {
      for (CtClass superType : interfaces) {
        logger.trace("interface: {}", superType.getName());

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
      logger.trace(e, "Resolving {} of class {}", member, cls.getName());
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
