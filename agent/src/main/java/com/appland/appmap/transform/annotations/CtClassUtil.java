package com.appland.appmap.transform.annotations;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Utility methods for working with CtClass and related types.
 */
public class CtClassUtil {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static String tracePrefix = Properties.DebugClassPrefix;

  /**
   * Checks whether or not two types are equal or related via class hierarchy or interface
   * implementation.
   *
   * @param candidateChildClass The child class
   * @param parentClassName     The name of the parent class
   * @return {@code true} if the parent class is a super class of the child or equal to the child.
   * Otherwise, {@code false}.
   */
  public static Boolean isChildOf(CtClass candidateChildClass, CtClass parentClass) {
    String childClassName = candidateChildClass.getName();
    boolean traceClass = tracePrefix == null || childClassName.startsWith(tracePrefix);
    String parentClassName = parentClass.getName();

    // It's important to do this check here (and in all the overloads below, of
    // course). isChildOf gets called during the initial transformation of the
    // class (i.e. before it gets added to the ClassPool), to decide whether the
    // its methods are eligible for hooking. Without this check, we'll try to
    // find it in the ClassPool. When that fails, none of its methods will get
    // hooked.
    if (childClassName.equals(parentClassName)) {
      return true;
    }

    CtClass[] interfaces = tryClass(candidateChildClass, "interfaces", candidateChildClass::getInterfaces);

    if (interfaces != null && interfaces.length > 0) {
      if (traceClass) {
        logger.trace("interfaces: {}",
            () -> Arrays.asList(interfaces).stream().map(c -> c.getName()).collect(Collectors.joining(",")));
      }
      for (CtClass superType : interfaces) {
        if (traceClass) {
          logger.trace(() -> String.format("interface: %s", superType.getName()));
        }

        if (superType.getName().equals(parentClassName)) {
          return true;
        }

        if (isChildOf(superType, parentClass)) {
          return true;
        }
      }
    } else {
      if (traceClass) {
        logger.trace("no interfaces");
      }
    }

    CtClass superClass = tryClass(candidateChildClass, "superclass", candidateChildClass::getSuperclass);
    if (traceClass) {
      logger.trace("superClass: {}", () -> superClass != null ? superClass.getName() : "null");
    }
    if (superClass == null) {
      return false;
    }
    // When we get to the top of the hierarchy, check to see if the parent is
    // java.lang.Object. If so, we've got a match, but either way, we're done
    // scanning.
    if (superClass.getName().equals("java.lang.Object")) {
      return parentClassName.equals("java.lang.Object");
    }

    return isChildOf(superClass, parentClass);
  }

  private static <V> V tryClass(CtClass cls, String member, ClassAccessor<V> accessor) {
    try {
      return accessor.navigate();
    } catch (NotFoundException e) {
      logger.trace(() -> String.format("Resolving %s of class %s", member, cls.getName()));
      return null;
    }
  }

  interface ClassAccessor<V> {
    V navigate() throws NotFoundException;
  }

  public static Boolean isChildOf(String childClassName, CtClass parentClass) {
    String parentClassName = parentClass.getName();
    boolean namesEqual = childClassName.equals(parentClassName);
    if (namesEqual)
      return true;

    ClassPool cp = ClassPool.getDefault();
    try {
      CtClass childClass = cp.get(childClassName);

      Boolean ret = isChildOf(childClass, parentClass);
      logger.trace(() -> String.format("[0]childClassName: %s, parentClassName: %s ret: %s", childClassName,
          parentClass.getName(), ret));
      return ret;
    } catch (NotFoundException e) {
      logger.trace(
          "[1]childClassName: {}, parentClassName: {} ret: false ({} not found)",
          childClassName,
          parentClassName,
          e.getMessage());
    }
    return false;
  }

  public static Boolean isChildOf(CtClass childClass, String parentClassName) {
    String childClassName = childClass.getName();
    boolean namesEqual = childClassName.equals(parentClassName);
    if (namesEqual)
      return true;

    ClassPool cp = ClassPool.getDefault();
    try {
      CtClass parentClass = cp.get(parentClassName);

      Boolean ret = isChildOf(childClass, parentClass);
      logger.trace(() -> String.format("[2]childClassName: %s, parentClassName: %s ret: %s", childClass.getName(),
          parentClassName, ret));
      return ret;
    } catch (NotFoundException e) {
      logger.trace(
          "[3]childClassName: {}, parentClassName: {} ret: false ({} not found)",
          childClassName,
          parentClassName,
          e.getMessage());
    }
    return false;
  }

  public static Boolean isChildOf(String childClassName, String parentClassName) {
    boolean namesEqual = childClassName.equals(parentClassName);
    if (namesEqual)
      return true;

    ClassPool cp = ClassPool.getDefault();
    try {
      CtClass parentClass = cp.get(parentClassName);

      Boolean ret = isChildOf(childClassName, parentClass);
      logger.trace(
          () -> String.format("[4]childClassName: %s, parentClassName: %s ret: %s", childClassName, parentClassName,
              ret));
      return ret;
    } catch (NotFoundException e) {
      logger.trace(
          "[5]childClassName: {}, parentClassName: {} ret: false ({} not found)",
          childClassName,
          parentClassName,
          e.getMessage());
    }
    return false;
  }

  public static Object isChildOf(Class<?> child, Class<?> parent) {
    if (child == null || parent == null) {
      return false;
    }

    return isChildOf(child.getName(), parent.getName());
  }

  /*
   * Trying to diagnose problems with isChildOf when instrumenting an app is
   * hard -- the huge number of classes loaded makes the log really noisy.
   * Having an entrypoint here allows us to investigate the relationship between
   * a single child class and a potential parent.
   */
  public static void main(String[] argv) {
    String child = argv[0];
    String parent = argv[1];
    logger.info("{} {}", child, parent);
    System.out.println(String.format("isChildOf(%s, %s): %b", child, parent, isChildOf(child, parent)));
  }
}
