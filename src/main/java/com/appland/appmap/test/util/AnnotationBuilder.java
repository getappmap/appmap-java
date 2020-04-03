package com.appland.appmap.test.util;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * Attaches Annotations to Methods created via the {@link MethodBuilder}.
 * @see MethodBuilder
 * @see ClassBuilder
 */
public class AnnotationBuilder {
  private MethodBuilder declaringMethodBuilder;
  private String typeName;
  private List<AnnotationMemberValue> annotationMemberValues =
      new ArrayList<AnnotationMemberValue>();

  private class AnnotationMemberValue {
    private String name;
    private Object value;

    AnnotationMemberValue(String name, Object value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return this.name;
    }

    public Object getValue() {
      return this.value;
    }
  }

  /**
   * Constructor. Typically you shouldn't be calling this outside of {@link MethodBuilder}.
   * @param declaringMethodBuilder The owning {@link MethodBuilder}
   */
  public AnnotationBuilder(MethodBuilder declaringMethodBuilder) {
    this.declaringMethodBuilder = declaringMethodBuilder;
  }

  /**
   * Set the Annotation type.
   * @param typeName The fully qualified name of the Annotation
   * @return {@code this}
   */
  public AnnotationBuilder setType(String typeName) {
    this.typeName = typeName;
    return this;
  }

  /**
   * Set an Annotation member variable.
   * @param name The name of the member variable
   * @param value The value of the member variable
   * @return {@code this}
   */
  public AnnotationBuilder setMember(String name, Object value) {
    this.annotationMemberValues.add(new AnnotationMemberValue(name, value));
    return this;
  }

  /**
   * Completes the Annotation.
   * @return The declaring {@link MethodBuilder}
   */
  public MethodBuilder endAnnotation() {
    return this.declaringMethodBuilder;
  }

  /**
   * Build the Annotation. You shouldn't have to call this outside of the {@link MethodBuilder}.
   * @param constPool The declaring method's Const Pool
   * @return The newly created Annotation
   * @throws CannotCompileException If an invalid member value is encountered.
   */
  public Annotation build(ConstPool constPool) throws CannotCompileException {
    Annotation annotation = new Annotation(this.typeName, constPool);

    for (AnnotationMemberValue annotationMemberValue : this.annotationMemberValues) {
      MemberValue memberValue;
      Object value = annotationMemberValue.getValue();

      switch (value.getClass().getName()) {
        case "boolean":
        case "java.lang.Boolean":
          memberValue = new BooleanMemberValue((Boolean) value, constPool);
          break;

        case "char":
        case "java.lang.Character":
          memberValue = new CharMemberValue((Character) value, constPool);
          break;

        case "byte":
        case "java.lang.Byte":
          memberValue = new ByteMemberValue((Byte) value, constPool);
          break;

        case "short":
        case "java.lang.Short":
          memberValue = new ShortMemberValue((Short) value, constPool);
          break;

        case "int":
        case "java.lang.Integer":
          memberValue = new IntegerMemberValue((Integer) value, constPool);
          break;

        case "long":
        case "java.lang.Long":
          memberValue = new LongMemberValue((Long) value, constPool);
          break;

        case "float":
        case "java.lang.Float":
          memberValue = new FloatMemberValue((Float) value, constPool);
          break;

        case "double":
        case "java.lang.Double":
          memberValue = new DoubleMemberValue((Double) value, constPool);
          break;

        case "java.lang.String":
          memberValue = new StringMemberValue((String) value, constPool);
          break;

        case "java.lang.Class":
          memberValue = new ClassMemberValue(((Class<?>)value).getName(), constPool);
          break;

        default:
          if (Enum.class.isInstance(value)) {
            EnumMemberValue enumValue = new EnumMemberValue(constPool);
            enumValue.setType(value.getClass().getName());
            enumValue.setValue(((Enum<?>) value).name());
            memberValue = enumValue;
          } else {
            throw new CannotCompileException("invalid member type " + value.getClass().getName());
          }
      }

      annotation.addMemberValue(annotationMemberValue.getName(), memberValue);
    }

    return annotation;
  }
}
