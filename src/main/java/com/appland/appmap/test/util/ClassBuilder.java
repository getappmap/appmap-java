package com.appland.appmap.test.util;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamically create Classes.
 */
public class ClassBuilder {
  private CtClass myClass;
  private List<MethodBuilder> methods = new ArrayList<MethodBuilder>();

  /**
   * Begins building a dynamic class.
   * @param className The name of the new class to be loaded
   */
  public ClassBuilder(String className) {
    this.myClass = ClassPool.getDefault().makeClass(className);
  }

  /**
   * Begin constructing a new method for this class.
   * @return A new {@link MethodBuilder} instance
   */
  public MethodBuilder beginMethod() {
    MethodBuilder builder = new MethodBuilder(this);
    this.methods.add(builder);
    return builder;
  }

  /**
   * Begin constructing a new method for this class.
   * @param methodName The name of the new method
   * @return A new {@link MethodBuilder} instance
   */
  public MethodBuilder beginMethod(String methodName) {
    return this
        .beginMethod()
        .setName(methodName);
  }

  /**
   * Get the CtClass instance before the class is loaded.
   * @return The CtClass being built
   */
  public CtClass ctClass() {
    return this.myClass;
  }

  /**
   * Finalizes the class and loads it.
   * @return A {@link NewClass} instance
   * @throws CannotCompileException If compilation fails
   */
  public NewClass build() throws CannotCompileException {
    return new NewClass(this.myClass, ClassPool.getDefault().toClass(this.myClass));
  }
}
