package com.appland.appmap.test.util;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.compiler.Javac;

public class MethodBuilder {
  private ClassBuilder declaringClassBuilder;
  private List<CtClass> exceptions = new ArrayList<CtClass>();
  private CtClass returnType = CtClass.voidType;
  private String name;
  private String body = "{ }";
  private Integer modifiers = Modifier.PUBLIC;
  private List<ParameterBuilder> parameters = new ArrayList<ParameterBuilder>();
  private List<AnnotationBuilder> annotations = new ArrayList<AnnotationBuilder>();

  public MethodBuilder(ClassBuilder declaringClassBuilder) {
    this.declaringClassBuilder = declaringClassBuilder;
  }

  public MethodBuilder setBody(String body) {
    this.body = body;
    return this;
  }

  public MethodBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public MethodBuilder setReturnType(CtClass returnType) {
    this.returnType = returnType;
    return this;
  }

  public MethodBuilder setReturnType(String returnTypeName) throws NotFoundException {
    this.setReturnType(ClassPool.getDefault().get(returnTypeName));
    return this;
  }

  public MethodBuilder addException(CtClass exceptionType) {
    this.exceptions.add(exceptionType);
    return this;
  }

  public MethodBuilder addException(String exceptionTypeName) throws NotFoundException {
    this.addException(ClassPool.getDefault().get(exceptionTypeName));
    return this;
  }

  public MethodBuilder addModifer(Integer modifier) {
    this.modifiers |= modifier;
    return this;
  }

  public MethodBuilder addParameter(CtClass parameterType, String parameterId) {
    this.beginParameter()
        .setType(parameterType)
        .setId(parameterId)
        .endParameter();

    return this;
  }

  public MethodBuilder addParameter(String parameterType, String parameterId)
      throws NotFoundException {
    this.beginParameter()
        .setType(parameterType)
        .setId(parameterId)
        .endParameter();
    return this;
  }

  public ParameterBuilder beginParameter() {
    ParameterBuilder builder = new ParameterBuilder(this);
    this.parameters.add(builder);
    return builder;
  }

  public AnnotationBuilder beginAnnotation() {
    AnnotationBuilder builder = new AnnotationBuilder(this);
    this.annotations.add(builder);
    return builder;
  }

  public AnnotationBuilder beginAnnotation(String annotationType) {
    return this.beginAnnotation().setType(annotationType);
  }

  public MethodBuilder addAnnotation(String annotationType) {
    return this.beginAnnotation(annotationType)
               .endAnnotation();
  }

  public ClassBuilder endMethod() throws CannotCompileException {
    this.build();
    return this.declaringClassBuilder;
  }

  private CtMethod build() throws CannotCompileException {
    final CtClass[] parameters = this.parameters
    .stream()
    .map(p -> p.getType())
    .toArray(CtClass[]::new);

    Integer paramIndex = 1;
    for (ParameterBuilder parameterBuilder : this.parameters) {
      this.body = parameterBuilder.getType().getName()
          + " "
          + parameterBuilder.getId()
          + " = $"
          + paramIndex
          + "; "
          + this.body;
      paramIndex += 1;
    }

    this.body = "{ " + this.body + " }";

    final CtClass[] exceptions = new CtClass[this.exceptions.size()];
    this.exceptions.toArray(exceptions);

    final CtMethod method = CtNewMethod.make(
        this.modifiers,
        this.returnType,
        this.name,
        parameters,
        exceptions,
        this.body,
        this.declaringClassBuilder.ctClass());

    final CodeAttribute codeAttribute = method.getMethodInfo().getCodeAttribute();
    final ConstPool constPool = method.getMethodInfo().getConstPool();
    LocalVariableAttribute locals = new LocalVariableAttribute(constPool);
    paramIndex = 1;
    for (ParameterBuilder parameterBuilder : this.parameters) {
      Integer nameIndex = constPool.addUtf8Info(parameterBuilder.getId());
      Integer descIndex = constPool.addUtf8Info(Descriptor.of(parameterBuilder.getType()));
      locals.addEntry(0, codeAttribute.getCodeLength(), nameIndex, descIndex, paramIndex);
      paramIndex += 1;
    }

    codeAttribute.getAttributes().add(locals);


    final AnnotationsAttribute annotationAttribute =
          new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

    for (AnnotationBuilder annotationBuilder : this.annotations) {
      Annotation annotation = annotationBuilder.build(constPool);
      annotationAttribute.addAnnotation(annotation);
    }

    method.getMethodInfo().addAttribute(annotationAttribute);
    this.declaringClassBuilder.ctClass().addMethod(method);

    return method;
  }
}