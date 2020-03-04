package com.appland.appmap.output.v1;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SignatureAttribute.Type;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parameters implements Iterable<Value> {
  private static final Integer MAX_ARGS = 255;
  private final ArrayList<Value> values = new ArrayList<Value>();

  public Parameters() { }

  public Parameters(CtBehavior behavior) {
    MethodInfo methodInfo = behavior.getMethodInfo();
    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    if (codeAttribute == null) {
      throw new NoSourceAvailableException();
    }

    LocalVariableAttribute locals = (LocalVariableAttribute) codeAttribute.getAttribute(
        javassist.bytecode.LocalVariableAttribute.tag);

    if (locals == null) {
      throw new NoSourceAvailableException();
    }

    Integer numberLocals = locals.tableLength();
    CtClass[] parameterTypes = new CtClass[]{};

    try {
      parameterTypes = behavior.getParameterTypes();
    } catch (NotFoundException e) {
      System.err.println(
          String.format("failed to get parameter types for %s.%s: %s",
              behavior.getDeclaringClass().getName(),
              behavior.getName(),
              e.getMessage()));
    }

    Boolean isStatic = (behavior.getModifiers() & Modifier.STATIC) != 0;
    Value[] paramValues = new Value[parameterTypes.length];
    for (int i = 0; i < numberLocals; ++i) {
      // parameters are not neccesarily the first local variables
      Integer paramIndex = locals.index(i);

      if (!isStatic) {
        // index 0 is `this` for nonstatic methods
        // we don't need it
        if (paramIndex == 0) {
          continue;
        }

        // similarly, `parameterTypes` does not contain `this`, so shift our index back by one
        paramIndex -= 1;
      }

      if (paramIndex >= parameterTypes.length) {
        continue;
      }

      Value param = new Value()
          .setClassType(parameterTypes[paramIndex].getName())
          .setName(locals.variableName(i))
          .setKind("req");

      paramValues[paramIndex] = param;
    }

    for (int i = 0; i < paramValues.length; ++i) {
      this.add(paramValues[i]);
    }
  }

  @Override
  public Iterator<Value> iterator() {
    return this.values.iterator();
  }

  public boolean add(Value param) {
    if (param == null) {
      return false;
    }

    return this.values.add(param);
  }

  public Stream<Value> stream() {
    return this.values.stream();
  }

  public int size() {
    return this.values.size();
  }

  public void clear() {
    this.values.clear();
  }

  public Value get(String name) throws NoSuchElementException {
    if (this.values != null) {
      for (Value param : this.values) {
        if (param.name.equals(name)) {
          return param;
        }
      }
    }

    throw new NoSuchElementException();
  }

  public Value get(Integer index) throws NoSuchElementException {
    if (this.values == null) {
      throw new NoSuchElementException();
    }

    try {
      return this.values.get(index);
    } catch (NullPointerException | IndexOutOfBoundsException e) {
      throw new NoSuchElementException();
    }
  }

  public Boolean validate(Integer index, String type) {
    try {
      Value param = this.get(index);
      return param.classType.equals(type);
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  public Parameters clone() {
    Parameters clonedParams = new Parameters();
    for(Value param : this.values) {
      clonedParams.add(new Value(param));
    }
    return clonedParams;
  }

  @Override
  public String toString() {
    return this.values
        .stream()
        .map(value -> value.classType)
        .collect(Collectors.joining(", "));
  }
}