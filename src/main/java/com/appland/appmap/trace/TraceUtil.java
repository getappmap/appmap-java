package com.appland.appmap.trace;

class TraceUtil {
  private static Boolean isDebug = (System.getenv("APPMAP_DEBUG") != null);

  public static Boolean isDebugMode() {
    return isDebug;
  }

  public static String getSourcePath(Class classType) {
    String srcPath = classType.getName().replace('.', '/');
    return String.format("src/main/java/%s", srcPath);
  }
}
