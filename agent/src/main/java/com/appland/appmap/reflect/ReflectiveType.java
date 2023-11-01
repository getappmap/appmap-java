package com.appland.appmap.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;

/* ReflectiveType implements some simple duck typing. As long as self
 * implements the methods required by subclasses of ReflectiveType, the actual
 * type of self is immaterial. For example,
 * com.appland.appmap.reflect.HttpServletRequest can wrap objects that implement
 * either javax.servlet.HttpServletRequest, or
 * jakarta.servlet.http.HttpServletRequest. 
 */
public class ReflectiveType {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private Map<String, Method> methods = new HashMap<String, Method>();

  protected Object self;

  public ReflectiveType(Object self) {
    if (self == null) {
      throw new InternalError("self must not be null");
    }

    this.self = self;
    addMethods("hashCode", "toString");
    addMethod("equals", Object.class);
  }

  public int hashCode() {
    return invokeIntMethod("hashCode");
  }

  public String toString() {
    return invokeStringMethod("toString");
  }

  public boolean equals(Object other) {
    return invokeMethod("equals", Boolean.FALSE, other);
  }

  protected ClassLoader getClassLoader() {
    return self.getClass().getClassLoader();
  }

  /* Add no-argument methods that can be called on self */
  protected void addMethods(String... names) {
    for (String name : names) {
      this.methods.put(name, getMethod(name));
    }
  }

  /**
   * Add a method by name and parameter types
   *
   * @return {@code true} if the method was found in self
   */
  protected boolean addMethod(String name, Class<?>... parameterTypes) {
    Method m = getMethod(name, parameterTypes);
    if (m != null) {
      this.methods.put(name, m);
      return true;
    }

    return false;
  }

  protected Method getMethod(String name, Class<?>... parameterTypes) {
    final Class<?> cls = self.getClass();
    try {
      return cls.getMethod(name, parameterTypes);
    } catch (Exception e) {
      logger.trace(e, "failed to get public method {}.{}", cls.getName(), name);
    }

    logger.debug("failed to get method {}.{}", cls.getName(), name);
    return null;
  }

  /**
   * Add a method by name and parameter types, also by name
   *
   * @return {@code true} if the method was found in self
   */
  protected boolean addMethod(String name, String... parameterTypes) {
    Method m = getMethodByClassNames(name, parameterTypes);
    if (m != null) {
      this.methods.put(name, m);
      return true;
    }
    return false;
  }

  protected boolean hasMethod(String name) {
    return this.methods.get(name) != null;
  }

  protected Object invokeWrappedMethod(Method method, Object... parameters) {
    try {
      method.setAccessible(true);
      logger.trace("method: {} parameters: {}", method, parameters);
      return method.invoke(self, parameters);
    } catch (InvocationTargetException e) {
      logger.warn(e, "{}.{} threw an exception", self.getClass().getName(), method.getName());
      throw new Error(e);
    } catch (Exception e) {
      logger.warn(e, "failed invoking {}.{}", self.getClass().getName(), method.getName());
      if (e.getCause() != null) {
        logger.warn(e.getCause());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  protected <T> T invokeMethod(String name, T defaultValue, Object... parameters) {
    Method m = methods.get(name);
    if (m == null) {
      logger.debug("method {} not found in {}, did you forget to call addMethod?", name, self.getClass().getName());
    }
    return m != null ? (T) invokeWrappedMethod(m, parameters)
        : defaultValue;
  }

  protected String invokeStringMethod(String name, Object... parameters) {
    return invokeMethod(name, "", parameters);
  }

  protected Integer invokeIntMethod(String name, Object... parameters) {
    return invokeMethod(name, -1, parameters);
  }

  protected Object invokeObjectMethod(String name, Object... parameters) {
    return invokeMethod(name, null, parameters);
  }

  protected void invokeVoidMethod(String name, Object... parameters) {
    invokeMethod(name, null, parameters);
  }

  /**
   * Looks up a method signature by class names.
   * 
   * @param name               Method name
   * @param parameterTypeNames Fully qualified class names of all parameters
   * @return Matching method if found. Otherwise, null.
   */
  protected Method getMethodByClassNames(String name, String... parameterTypeNames) {
    Class<?> selfClass = self.getClass();
    logger.trace("self.getClass(): {}", selfClass);

    try {
      final List<Class<?>> parameterTypes = new ArrayList<Class<?>>();
      ClassLoader cl = selfClass.getClassLoader();
      for (String typeName : parameterTypeNames) {
        parameterTypes.add(Class.forName(typeName, true, cl));
      }
      return selfClass.getMethod(name, parameterTypes.toArray(new Class<?>[0]));
    } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
      logger.debug(e, "No match for method {}", name);
    }

    return null;
  }
}
