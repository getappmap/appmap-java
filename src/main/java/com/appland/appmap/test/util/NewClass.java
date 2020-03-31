package com.appland.appmap.test.util;

import javassist.CtClass;

public class NewClass {
  private CtClass ctClass;
  private Class<?> reflectClass;

  public NewClass(CtClass ctClass, Class<?> reflectClass) {
    this.ctClass = ctClass;
    this.reflectClass = reflectClass;
  }

  public CtClass asCtClass() {
    return this.ctClass;
  }

  public Class<?> asClass() {
    return this.reflectClass;
  }
}