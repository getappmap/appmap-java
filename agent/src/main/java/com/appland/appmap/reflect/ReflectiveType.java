package com.appland.appmap.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;

/* ReflectiveType implements some simple ducking typing. As long as self
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

  /* Add no-argument methods that can be called on self */
  protected void addMethods(String... names) {
    for (String name : names) {
      this.methods.put(name, getMethod(name));
    }
  }

  /* Add a method that takes parameters that can be called on self */
  protected void addMethod(String name, Class<?>... parameterTypes) {
    this.methods.put(name, getMethod(name, parameterTypes));
  }

  protected Method getMethod(String name, Class<?>... parameterTypes) {
    final Class<?> cls = self.getClass();
    try {
      return cls.getMethod(name, parameterTypes);
    } catch (Exception e) {
      logger.warn(e, "failed to get method {}.{}", cls.getName(), name);
      return null;
    }
  }

  protected void addMethod(String name, String... parameterTypes) {
    this.methods.put(name, getMethodByClassNames(name, parameterTypes));
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
   * @param name Method name
   * @param parameterTypes Fully qualified class names of all parameters
   * @return Matching method if found. Otherwise, null.
   */
  protected Method getMethodByClassNames(String name, String... parameterTypes) {
    Method[] methods;

    logger.trace("self.getClass(): {}", self.getClass());

    try {
      methods = this.self.getClass().getMethods();
    } catch (Exception e) {
      logger.warn(e, "failed to get methods for {}", this.self.getClass().getName());
      return null;
    }

    for (Method method : methods) {
      logger.trace("method: {}", method.getName());
      if (!method.getName().equals(name)) {
        continue;
      }

      final Class<?>[] methodParamTypes = method.getParameterTypes();
      if (methodParamTypes.length != parameterTypes.length) {
        logger.trace("parameter type lengths don't match");
        continue;
      }

      boolean match = true;
      for (int i = 0; i < methodParamTypes.length; i++) {
        final String actual = methodParamTypes[i].getName();
        final String expected = parameterTypes[i];
        logger.trace("actual: \"{}\" expected: \"{}\"", actual, expected);

        if (!actual.equals(expected)) {
          match = false;
          break;
        }
      }

      if (match) {
        return method;
      }
    }

    logger.warn("No match for method {}", name);
    return null;
  }
}
