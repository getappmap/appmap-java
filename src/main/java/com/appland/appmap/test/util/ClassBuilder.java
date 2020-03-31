package com.appland.appmap.test.util;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;

public class ClassBuilder {
  private CtClass myClass;
  private List<MethodBuilder> methods = new ArrayList<MethodBuilder>();

  public ClassBuilder(String className) {
    this.myClass = ClassPool.getDefault().makeClass(className);
  }

  public MethodBuilder beginMethod() {
    MethodBuilder builder = new MethodBuilder(this);
    this.methods.add(builder);
    return builder;
  }

  public MethodBuilder beginMethod(String methodName) {
    return this
        .beginMethod()
        .setName(methodName);
  }

  public CtClass ctClass() {
    return this.myClass;
  }

  public NewClass build() throws CannotCompileException {
    return new NewClass(this.myClass, ClassPool.getDefault().toClass(this.myClass));
  }
}