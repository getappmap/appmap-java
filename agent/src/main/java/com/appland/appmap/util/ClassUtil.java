package com.appland.appmap.util;

import static org.apache.commons.lang3.StringUtils.stripAll;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.CodeObject;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.MethodInfo;

public class ClassUtil {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  public static Class<?> safeClassForName(ClassLoader cl, String name) {
    try {
      return Class.forName(name, true, cl);
    } catch (ClassNotFoundException e) {
    }
    return null;
  }

  public static Class<?> safeClassForNames(ClassLoader cl, String... classNames) throws InternalError {
    Class<?> cls = null;
    for (String c : classNames) {
      if ((cls = safeClassForName(cl, c)) != null) {
        break;
      }
    }
    if (cls == null) {
      throw new InternalError("No class of " + String.join(",", classNames));
    }
    return cls;
  }

  private static Map<String, CtClass> primitiveTypeMap = new HashMap<>();
  static {
    primitiveTypeMap.put("boolean", CtClass.booleanType);
    primitiveTypeMap.put("char", CtClass.charType);
    primitiveTypeMap.put("byte", CtClass.byteType);
    primitiveTypeMap.put("short", CtClass.shortType);
    primitiveTypeMap.put("int", CtClass.intType);
    primitiveTypeMap.put("long", CtClass.longType);
    primitiveTypeMap.put("float", CtClass.floatType);
    primitiveTypeMap.put("double", CtClass.doubleType);
  }

  private static CtClass mapType(String name) {
    if (name == null || name.length() == 0) {
      return null;
    }

    CtClass ret = primitiveTypeMap.get(name);
    if (ret == null) {
      try {
        logger.trace("name: {}", name);
        ClassPool cp = AppMapClassPool.get();

        ret = cp.get(name);
      } catch (NotFoundException e) {
        logger.warn(e);
      }
    }

    return ret;
  }

  public static class MethodLocation {
    public String file;
    public int line;

    MethodLocation(String file, int line) {
      this.file = file;
      this.line = line;
    }
  }

  public static MethodLocation getMethodLocation(String className, String methodName, String paramTypes) {
    logger.trace("className: {}, methodName: {}, paramTypes: {}", className, methodName, paramTypes);

    CtClass cls;
    try {
      ClassPool cp = AppMapClassPool.get();

      cls = cp.get(className);
      String loc = CodeObject.getSourceFilePath(cls);
      logger.trace("loc: {}", loc);
      MethodInfo methodInfo = getDeclaredMethod(cls, methodName, paramTypes).getMethodInfo();
      LineNumberAttribute lineAttr = (LineNumberAttribute) methodInfo.getCodeAttribute()
          .getAttribute(LineNumberAttribute.tag);
      if (loc != null && methodInfo != null) {
        return new MethodLocation(loc, lineAttr.toLineNumber(0));
      }
    } catch (NotFoundException e) {
      logger.warn(e);
    }
    return null;
  }

  private static String[] splitParams(String paramTypes) {
    return stripAll(paramTypes.split(","));
  }

  public static CtMethod getDeclaredMethod(String className, String methodName, String paramTypes)
      throws NotFoundException {
    return getDeclaredMethod(className, methodName, splitParams(paramTypes));
  }

  public static CtMethod getDeclaredMethod(CtClass cls, String methodName, String paramTypes)
      throws NotFoundException {
    return getDeclaredMethod(cls, methodName, splitParams(paramTypes));
  }

  public static CtMethod getDeclaredMethod(String className, String methodName, String[] paramTypes)
      throws NotFoundException {
    ClassPool cp = AppMapClassPool.get();
    CtClass cls = cp.get(className);
    return getDeclaredMethod(cls, methodName, paramTypes);
  }

  public static CtMethod getDeclaredMethod(CtClass cls, String methodName, String[] paramTypes)
      throws NotFoundException {
    CtClass[] paramClasses = Arrays.stream(paramTypes)
        .map(t -> mapType(t))
        .filter(Objects::nonNull)
        .toArray(CtClass[]::new);
    return cls.getDeclaredMethod(methodName, paramClasses);
  }

  public static <E extends Enum<E>> E enumValueOf(Class<E> enumClass, String valueName) {
    try {
      E enumValue = Enum.valueOf(enumClass, valueName);
      return enumValue;
    } catch (IllegalArgumentException e) {
      throw new InternalError("failed to fetch " + enumClass.getName() + "." + valueName, e);
    }
  }

  /**
   * Return an EnumSet with a single Enum value named valueName, from the first
   * valid class found by classNames.
   */
  public static Enum<?> enumValueOf(ClassLoader cl, String valueName, String... classNames) {
    Class<?> cls = safeClassForNames(cl, classNames);
    return enumValueOf(cls.asSubclass(Enum.class), valueName);
  }

  @SuppressWarnings("all")
  public static EnumSet<?> enumSetOf(Class<?> enumClass, String valueName) {
    return EnumSet.of((Enum) enumValueOf(enumClass.asSubclass(Enum.class), valueName));
  }
}
