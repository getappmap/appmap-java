package com.appland.appmap.trace;

import java.lang.reflect.Modifier;
import javassist.CtBehavior;
import javassist.CtClass;

class TraceUtil {
  private static Boolean isDebug = (System.getenv("APPMAP_DEBUG") != null);

  public static Boolean isDebugMode() {
    return isDebug;
  }

  public static String getSourcePath(CtClass classType) {
    // best effort guess. at least it's unique and consistent.
    String srcPath = classType.getName().replace('.', '/');
    return String.format("src/main/java/%s.java", srcPath);
  }

  public static Boolean isRelevant(CtBehavior behavior) {
    if ((behavior.getModifiers() & Modifier.PUBLIC) == 0) {
      return false;
    }

    if (behavior.getMethodInfo().getLineNumber(0) == -1) {
      // auto generated code, cglib enhancers, etc.
      // it's totally possible this might remove something we care about
      // -db
      return false;
    }

    if (behavior.getName().contains("$")) {
      return false;
    }

    // hooking toString could cause a stack overflow
    if (behavior.getName().equals("toString")) {
      return false;
    }

    return true;
  }
}
