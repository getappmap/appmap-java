package com.appland.appmap.test.fixture;

import com.appland.appmap.test.util.ClassBuilder;

import javassist.CtClass;

/**
 * A data structure for dynamic class information after being built by a {@link ClassBuilder}.
 */
public class NewClass {
  private CtClass ctClass;
  private Class<?> reflectClass;

  public NewClass(CtClass ctClass, Class<?> reflectClass) {
    this.ctClass = ctClass;
    this.reflectClass = reflectClass;
  }

  /**
   * Get this class.
   * 
   * @return As a CtClass
   */
  public CtClass asCtClass() {
    return this.ctClass;
  }

  /**
   * Get this class.
   * 
   * @return As a System Class
   */
  public Class<?> asClass() {
    return this.reflectClass;
  }
}
