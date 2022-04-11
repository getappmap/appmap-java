package com.appland.appmap.test.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Define a new parameter for a method defined via the {@link MethodBuilder}.
 */
public class ParameterBuilder {
  private MethodBuilder declaringMethodBuilder;
  private String id;
  private CtClass type;

  /**
   * Constructor. Typically you shouldn't be calling this outside of {@link MethodBuilder}.
   * @param declaringMethodBuilder The declaring {@link MethodBuilder}
   */
  public ParameterBuilder(MethodBuilder declaringMethodBuilder) {
    this.declaringMethodBuilder = declaringMethodBuilder;
  }

  /**
   * Set the parameter type.
   * @param type The type of this parameter
   * @return {@code this}
   */
  public ParameterBuilder setType(CtClass type) {
    this.type = type;
    return this;
  }

  /**
   * Set the method return type.
   * @param typeName The fully qualified name of the parameter type
   * @return {@code this}
   * @throws NotFoundException If the name of the parameter type cannot be resolved to a Class
   */
  public ParameterBuilder setType(String typeName) throws NotFoundException {
    this.setType(ClassPool.getDefault().get(typeName));
    return this;
  }

  /**
   * Set the identifier of this parameter.
   * @param id The identifier of this parameter.
   * @return {@code this}
   */
  public ParameterBuilder setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get the identifier of this parameter.
   * @return The identifier
   */
  public String getId() {
    return this.id;
  }

  /**
   * Get the type of this parameter.
   * @return The type
   */
  public CtClass getType() {
    return this.type;
  }

  /**
   * Completes the parameter.
   * @return The declaring {@link MethodBuilder}
   */
  public MethodBuilder endParameter() {
    return this.declaringMethodBuilder;
  }
}
