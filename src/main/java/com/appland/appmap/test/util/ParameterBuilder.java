package com.appland.appmap.test.util;

import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class ParameterBuilder {
  private MethodBuilder declaringMethodBuilder;
  private String id;
  private CtClass type;

  public ParameterBuilder(MethodBuilder declaringMethodBuilder) {
    this.declaringMethodBuilder = declaringMethodBuilder;
  }

  public ParameterBuilder setType(CtClass type) {
    this.type = type;
    return this;
  }

  public ParameterBuilder setType(String typeName) throws NotFoundException {
    this.setType(ClassPool.getDefault().get(typeName));
    return this;
  }

  public ParameterBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public String getId() {
    return this.id;
  }

  public CtClass getType() {
    return this.type;
  }

  public MethodBuilder endParameter() {
    return this.declaringMethodBuilder;
  }
}