package com.appland.appmap.output.v1;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.util.Logger;

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

    CtClass[] paramTypes = null;
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

    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    LocalVariableAttribute locals = null;
    if (codeAttribute != null) {
      locals = (LocalVariableAttribute) codeAttribute.getAttribute(javassist.bytecode.LocalVariableAttribute.tag);
    } else {
      logger.debug("No code attribute for {}", fqn);
    }

    int numParams = paramTypes.length;
    String[] paramNames = new String[numParams];
    if (locals != null && numParams > 0) {
      int numLocals = locals.tableLength();

      // This is handy when debugging this code, but produces too much
      // noise for general use.
      if (Properties.DebugLocals) {
        logger.debug("local variables for {}", fqn);
        for (int idx = 0; idx < numLocals; idx++) {
          logger.debug("  {} {} {}", idx, locals.variableName(idx), locals.index(idx));
        }
      }

      // Iterate through the local variables to find the ones that match the argument slots.
      // Arguments are pushed into consecutive slots, starting at 0 (for this or the first argument),
      // and then incrementing by 1 for each argument, unless the argument is an unboxed long or double,
      // in which case it takes up two slots.
      int slot = Modifier.isStatic(behavior.getModifiers()) ? 0 : 1; // ignore `this`
      for (int i = 0; i < numParams; i++) {
        try {
          // note that the slot index is not the same as the
          // parameter index or the local variable index
          paramNames[i] = locals.variableNameByIndex(slot);
        } catch (Exception e) {
          // the debug info might be corrupted or partial, let's not crash in this case
          logger.debug(e, "Failed to get local variable name for slot {} in {}", slot, fqn);
        } finally {
          // note these only correspond to unboxed types — boxed double and long will still have width 1
          int width = paramTypes[i] == CtClass.doubleType || paramTypes[i] == CtClass.longType ? 2 : 1;
          slot += width;
        }
      }
    }

    Value[] paramValues = new Value[numParams];
    for (int i = 0; i < paramTypes.length; ++i) {
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
