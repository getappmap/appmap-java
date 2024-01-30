package com.appland.appmap.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javassist.CtBehavior;

public class FullyQualifiedName {
  public final String packageName;
  public final String className;
  public final boolean isStatic;
  public final String methodName;

  public FullyQualifiedName(String packageName, String className, boolean isStatic,
      String methodName) {
    this.packageName = packageName != null? packageName : "";
    this.className = className;
    this.isStatic = isStatic;
    this.methodName = methodName;
  }

  public FullyQualifiedName(CtBehavior behavior) {
    this(behavior.getDeclaringClass().getPackageName(),
        behavior.getDeclaringClass().getSimpleName(), Modifier.isStatic(behavior.getModifiers()),
        behavior.getMethodInfo().getName());
  }

  public FullyQualifiedName(Method method) {
    this(method.getDeclaringClass().getPackage().getName(),
        method.getDeclaringClass().getSimpleName(),
        Modifier.isStatic(method.getModifiers()),
        method.getName());
  }

  public FullyQualifiedName(FullyQualifiedName fqn) {
    this(fqn.packageName, fqn.className, fqn.isStatic, fqn.methodName);
  }

  public String getClassName() {
    return packageName.length() > 0 ? packageName + "." + className : className;
  }

  public String methodSpec() {
    // This intentionally omits packageName
    return StringUtil.canonicalName(className, isStatic, methodName);
  }

  public String toString() {
    return StringUtil.canonicalName(packageName, className, isStatic, methodName);
  }
}
