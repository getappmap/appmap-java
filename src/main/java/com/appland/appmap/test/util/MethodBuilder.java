package com.appland.appmap.test.util;

import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Define a new method for a dynamic Class built via the {@link ClassBuilder}.
 */
public class MethodBuilder {
  private ClassBuilder declaringClassBuilder;
  private List<CtClass> exceptions = new ArrayList<CtClass>();
  private CtClass returnType = CtClass.voidType;
  private String name;
  private String body = "{ }";
  private Integer modifiers = Modifier.PUBLIC;
  private List<ParameterBuilder> parameters = new ArrayList<ParameterBuilder>();
  private List<AnnotationBuilder> annotations = new ArrayList<AnnotationBuilder>();

  /**
   * Constructor. Typically you shouldn't be calling this outside of {@link ClassBuilder}.
   * @param declaringClassBuilder The declaring {@link ClassBuilder}
   */
  public MethodBuilder(ClassBuilder declaringClassBuilder) {
    this.declaringClassBuilder = declaringClassBuilder;
  }

  /**
   * Set the method body. Defaults to empty ({@code &#123; &#125;}).
   * @param body The method body
   * @return {@code this}
   */
  public MethodBuilder setBody(String body) {
    this.body = body;
    return this;
  }

  /**
   * Set the method name.
   * @param name The name of the method
   * @return {@code this}
   */
  public MethodBuilder setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Set the method return type.
   * @param returnType The method return type
   * @return {@code this}
   */
  public MethodBuilder setReturnType(CtClass returnType) {
    this.returnType = returnType;
    return this;
  }

  /**
   * Set the method return type.
   * @param returnTypeName The name of the method return type
   * @return {@code this}
   * @throws NotFoundException If the name of the method return type cannot be resolved to a Class
   */
  public MethodBuilder setReturnType(String returnTypeName) throws NotFoundException {
    this.setReturnType(ClassPool.getDefault().get(returnTypeName));
    return this;
  }

  /**
   * Add an exception type that this method will throw.
   * @param exceptionType The exception type to add
   * @return {@code this}
   */
  public MethodBuilder addException(CtClass exceptionType) {
    this.exceptions.add(exceptionType);
    return this;
  }

  /**
   * Add an exception type that this method will throw.
   * @param exceptionTypeName The name of the exception type
   * @return {@code this}
   * @throws NotFoundException If the name of the exception type cannot be resolved to a Class
   */
  public MethodBuilder addException(String exceptionTypeName) throws NotFoundException {
    this.addException(ClassPool.getDefault().get(exceptionTypeName));
    return this;
  }

  /**
   * Add a modifier to this method such as {@code public} or {@code static}.
   * @param modifier The modifier flag to add
   * @return {@code this}
   */
  public MethodBuilder addModifer(Integer modifier) {
    this.modifiers |= modifier;
    return this;
  }

  /**
   * Add new parameter to this method. Parameter order is maintained.
   * @param parameterType The parameter type
   * @return {@code this}
   */
  public MethodBuilder addParameter(CtClass parameterType, String parameterId) {
    this.beginParameter()
        .setType(parameterType)
        .setId(parameterId)
        .endParameter();

    return this;
  }

  /**
   * Add new parameter to this method. Parameter order is maintained.
   * @param parameterType The name of the parameter type
   * @return {@code this}
   * @throws NotFoundException If the name of the parameter type cannot be resolved to a Class
   */
  public MethodBuilder addParameter(String parameterType, String parameterId)
      throws NotFoundException {
    this.beginParameter()
        .setType(parameterType)
        .setId(parameterId)
        .endParameter();
    return this;
  }

  /**
   * Begin building a new parameter. {@link ParameterBuilder#endParameter()} must be called to
   * return to building this method.
   * @return A new {@link ParameterBuilder}
   * @see ParameterBuilder
   */
  public ParameterBuilder beginParameter() {
    ParameterBuilder builder = new ParameterBuilder(this);
    this.parameters.add(builder);
    return builder;
  }

  /**
   * Begin building a new annotation bound to this method. {@link AnnotationBuilder#endAnnotation()}
   * must be called to return to building this method.
   * @return A new {@link AnnotationBuilder}
   * @see AnnotationBuilder
   */
  public AnnotationBuilder beginAnnotation() {
    AnnotationBuilder builder = new AnnotationBuilder(this);
    this.annotations.add(builder);
    return builder;
  }

  /**
   * Begin building a new annotation bound to this method. {@link AnnotationBuilder#endAnnotation()}
   * must be called to return to building this method.
   * @param annotationType The type of Annotation to be built
   * @return A new {@link AnnotationBuilder}
   * @see AnnotationBuilder
   */
  public AnnotationBuilder beginAnnotation(String annotationType) {
    return this.beginAnnotation().setType(annotationType);
  }

  /**
   * Begin building a new annotation bound to this method. {@link AnnotationBuilder#endAnnotation()}
   * must be called to return to building this method.
   * @param annotationType The fully qualified class name of the Annotation to be built
   * @return A new {@link AnnotationBuilder}
   * @see AnnotationBuilder
   */
  public MethodBuilder addAnnotation(String annotationType) {
    return this.beginAnnotation(annotationType)
               .endAnnotation();
  }

  /**
   * Completes the Method.
   * @return The declaring {@link ClassBuilder}
   * @throws CannotCompileException If method compilation fails
   */
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
