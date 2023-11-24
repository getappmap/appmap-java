package com.appland.appmap.classloading;

import java.io.IOException;
import com.appland.appmap.util.ClassUtil;

public class TestSafeClassForName implements TestClass {
  private static final String FILTER_CLASS_NAME = "javax.servlet.Filter";

  @Override
  public int beforeTest() throws Exception {
    // Sanity check
    try {
      Class.forName(FILTER_CLASS_NAME);
      System.err.println("Misconfigured, " + FILTER_CLASS_NAME + " shouldn't be on class path");
      return 1;
    }
    catch (ClassNotFoundException e) {
      // expected
    }
    return 0;
  }

  @Override
  public int runTest() throws IOException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    
    Class<?> filterClass = ClassUtil.safeClassForName(cl, FILTER_CLASS_NAME);
    return filterClass != null ? 0 : 1;
  }

}
