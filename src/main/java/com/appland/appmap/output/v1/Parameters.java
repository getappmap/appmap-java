package com.appland.appmap.output.v1;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A serializable list of named and typed objects.
 *
 * @see Event
 * @see Value
 * @see <a href="https://github.com/applandinc/appmap#parameter-object-format">GitHub: AppMap - Parameter object format</a>
 */
public class Parameters implements Iterable<Value> {
  private final ArrayList<Value> values = new ArrayList<Value>();

  public Parameters() { }

  /**
   * Constructs a Parameters object from an existing CtBehavior. Values will automatically be added,
   * named and typed after the behaviors parameters.
   *
   * @param behavior The behavior to construct Parameters from
   * @throws NoSourceAvailableException If parameter names cannot be read from the behavior
   * @see <a href="https://github.com/applandinc/appmap#function-call-attributes">GitHub: AppMap - Function call attributes</a>
   */
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

  /**
   * Get an iterator for each {@link Value}.
   * @return A {@link Value} iterator
   */
  @Override
  public Iterator<Value> iterator() {
    return this.values.iterator();
  }

  /**
   * Add a new {@link Value} object to the end of the list.
   * @param param The {@link Value} to be added
   */
  public boolean add(Value param) {
    if (param == null) {
      return false;
    }

    return this.values.add(param);
  }

  /**
   * Gets a stream of Values.
   * @return A {@link Value} Stream
   */
  public Stream<Value> stream() {
    return this.values.stream();
  }

  /**
   * Gets the number of values stored.
   * @return The size of the internal value array
   */
  public int size() {
    return this.values.size();
  }


  /**
   * Clears the internal value array.
   */
  public void clear() {
    this.values.clear();
  }

  /**
   * Gets a {@Value} object stored by this Parameters object by name/identifier.
   * @param name The name or identifier of the @{link Value} to be returned
   * @return The {@link Value} object found
   * @throws NoSuchElementException If no @{link Value} object is found
   */
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

  /**
   * Gets a {@Value} object stored by this Parameters object by index.
   * @param index The index of the @{link Value} to be returned
   * @return The {@link Value} object at the given index
   * @throws NoSuchElementException if no @{link Value} object is found at the given index
   */
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

  /**
   * Tests if the {@link Value} object at the given index is of a given type.
   * @param index The index to validate
   * @param type The name of the type to check for
   * @return {@code true} if the @{link Value} at the given index matches the type given. Otherwise,
   *         {@code false}.
   */
  public Boolean validate(Integer index, String type) {
    try {
      Value param = this.get(index);
      return param.classType.equals(type);
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  /**
   * Performs a deep copy of the Parameters object and all of its values.
   * @return A new Parameters object
   */
  public Parameters clone() {
    Parameters clonedParams = new Parameters();
    for (Value param : this.values) {
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
