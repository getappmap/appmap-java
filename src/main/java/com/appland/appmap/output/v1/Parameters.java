package com.appland.appmap.output.v1;

import com.appland.appmap.util.Logger;
import com.appland.appmap.config.Properties;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodParametersAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.AttributeInfo;

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
    String fqn = behavior.getDeclaringClass().getName() +
      "." + behavior.getName() +
      methodInfo.getDescriptor();

    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    if (codeAttribute == null) {
      throw new NoSourceAvailableException("No code attribute for " + fqn);
    }
           
    LocalVariableAttribute locals = (LocalVariableAttribute) codeAttribute.getAttribute(
        javassist.bytecode.LocalVariableAttribute.tag);

    // We should be able to handle methods without debug
    // information. However, as of 20200822, other errors come up if
    // do hook them, so bail out here.
    if (locals == null) {
      throw new NoSourceAvailableException("No local variables for " + fqn);
    }

    CtClass[] paramTypes = null;
    try {
      paramTypes = behavior.getParameterTypes();
    } catch (NotFoundException e) {
      throw new NoSourceAvailableException(
        String.format("Failed to get parameter types for %s: %s",
                      fqn, e.getMessage()));
    }

    String[] paramNames = null;
    int numParams = paramTypes.length;
    if (numParams > 0) {
      int numLocals = locals.tableLength();
      
      // This is handy when debugging this code, but produces too much
      // noise for general use.
      if (Properties.DebugLocals) {
        Logger.println("local variables for " + fqn);
        for (int idx = 0; idx < numLocals; idx++) {
          Logger.printf("  %d %s %d\n", idx, locals.variableName(idx), locals.index(idx));
        }
      }

      paramNames = new String[numParams];
      Boolean isStatic = (behavior.getModifiers() & Modifier.STATIC) != 0;
      int firstParamIdx = isStatic ? 0 : 1; // ignore `this`
      int localVarIdx = 0;
      
      // Scan the local variables until we find the one with an index
      // that matches the first parameter index.
      //
      // In some cases, though, there aren't local variables for the
      // parameters. For example, the class file for
      // org.springframework.samples.petclinic.owner.PetTypeFormatter,
      // has the method
      // print(Ljava/lang/Object;Ljava/util/Locale;)Ljava/lang/String
      // in it, which only has a local variable for `this`. This
      // method isn't in the source file, so I'm not sure where it's
      // coming from.
      for (; localVarIdx < numLocals; localVarIdx++) {
        if (locals.index(localVarIdx) == firstParamIdx)
          break;
      }

      if (localVarIdx < numLocals) {
        // Assume the rest of the parameters follow the first.
        paramNames[0] = locals.variableName(localVarIdx);
        for (int idx = 1; idx < numParams; idx++)
          paramNames[idx] = locals.variableName(localVarIdx + idx);
      }
    }

    Value[] paramValues = new Value[numParams];
    for (int i = 0; i < paramTypes.length; ++i) {
      // Use a real parameter name if we have it, a fake one if we
      // don't.
      String paramName = paramNames != null? paramNames[i] : "p" + i;
      Value param = new Value()
          .setClassType(paramTypes[i].getName())
          .setName(paramName)
          .setKind("req");

      paramValues[i] = param;
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
