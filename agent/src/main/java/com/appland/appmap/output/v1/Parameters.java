package com.appland.appmap.output.v1;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javassist.bytecode.AttributeInfo;
import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * A serializable list of named and typed objects.
 *
 * @see Event
 * @see Value
 * @see <a href="https://github.com/applandinc/appmap#parameter-object-format">GitHub: AppMap - Parameter object format</a>
 */
public class Parameters implements Iterable<Value> {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private final ArrayList<Value> values = new ArrayList<>();

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

    CtClass[] paramTypes;
    try {
      paramTypes = behavior.getParameterTypes();
    } catch (NotFoundException e) {
      //@formatter:off
      // getParameterTypes throws NotFoundException when it can't find a class
      // definition for the type of one of the parameters. Given that:
      //
      //   * the method represented by this behavior was loaded from one of the
      //     app's classes (and so originally compiled successfully)
      //
      //   * the JVM resolves the classes for a method's parameters before
      //     loading the method itself
      //
      // this failure can only happen if we're trying to instrument a method
      // that can never be called.
      //
      // Log a message and bail.
      //@formatter:on
      logger.debug(e, "Failed to get parameter types for {}", fqn);
      return;
    }

    String[] paramNames = getParameterNames(methodInfo, paramTypes);
    int numParams = paramTypes.length;

    Value[] paramValues = new Value[numParams];
    for (int i = 0; i < numParams; ++i) {
      // Use a real parameter name if we have it, a fake one if we
      // don't.
      String paramName = paramNames[i];
      if (paramName == null) {
        paramName = "p" + i;
      }
      Value param = new Value()
          .setClassType(paramTypes[i].getName())
          .setName(paramName)
          .setKind("req");

      paramValues[i] = param;
    }

    for (Value paramValue : paramValues) {
      this.add(paramValue);
    }
  }

  /**
   * Iterate through the LocalVariableTables to get parameter names.
   * Local variable tables are debugging metadata containing information about local variables.
   * Variables are organized into slots; first slots are used for parameters, then for local variables.
   *
   * @param methodInfo for the method
   * @param paramTypes types of the parameters (used to calculate slot positions)
   * @return Array of parameter names (ignoring this), with null for any names that could not be determined.
   * Length of the array matches length of paramTypes.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.13">The Java Virtual Machine Specification: The LocalVariableTable Attribute</a>
   */
  private static String[] getParameterNames(MethodInfo methodInfo, CtClass[] paramTypes) {
    String[] paramNames = new String[paramTypes.length];

    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    if (codeAttribute != null) {
      boolean isStatic = Modifier.isStatic(methodInfo.getAccessFlags());

      // count number of slots taken by all the parameters
      int slotCount = isStatic ? 0 : 1; // account for `this`
      for (CtClass paramType : paramTypes) {
        slotCount += (paramType == CtClass.doubleType || paramType == CtClass.longType) ? 2 : 1;
      }

      String[] namesBySlot = new String[slotCount];

      for (AttributeInfo attr : codeAttribute.getAttributes()) {
        if (attr instanceof LocalVariableAttribute) {
          LocalVariableAttribute localVarAttr = (LocalVariableAttribute) attr;

          for (int i = 0; i < localVarAttr.tableLength(); i++) {
            int index = localVarAttr.index(i);
            if (index < slotCount) {
              namesBySlot[index] = localVarAttr.variableName(i);
            }
          }
        }
      }

      int slot = isStatic ? 0 : 1; // ignore `this`
      for (int i = 0; i < paramTypes.length; i++) {
        paramNames[i] = namesBySlot[slot];
        int width = paramTypes[i] == CtClass.doubleType || paramTypes[i] == CtClass.longType ? 2 : 1;
        slot += width;
      }
    }

    return paramNames;
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
   * Gets a {@link Value} object stored by this Parameters object by name/identifier.
   * @param name The name or identifier of the @{link Value} to be returned
   * @return The {@link Value} object found
   * @throws NoSuchElementException If no {@link Value} object is found
   */
  public Value get(String name) throws NoSuchElementException {
    for (Value param : this.values) {
      if (param.name.equals(name)) {
        return param;
      }
    }

    throw new NoSuchElementException();
  }

  /**
   * Gets a {@link Value} object stored by this Parameters object by index.
   * @param index The index of the @{link Value} to be returned
   * @return The {@link Value} object at the given index
   * @throws NoSuchElementException if no {@link Value} object is found at the given index
   */
  public Value get(Integer index) throws NoSuchElementException {
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
   * Creates a copy of the parameters object with the value types, kinds and names preserved.
   * @return A new Parameters object
   */
  public Parameters freshCopy() {
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
