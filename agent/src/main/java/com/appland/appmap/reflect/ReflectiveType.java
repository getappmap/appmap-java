package com.appland.appmap.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.util.Logger;

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
      Logger.printf("failed to get method %s.%s: %s\n", cls.getName(), name, e.getMessage());
      return null;
    }
  }

  protected Object invokeWrappedMethod(Method method, Object... parameters) {
    try {
      method.setAccessible(true);
      return method.invoke(self, parameters);
    } catch (InvocationTargetException e) {
      Throwable thrown = e.getTargetException();
      final String msg = String.format("%s.%s threw an exception, %s\n", self.getClass().getName(), method.getName(),
          thrown != null ? thrown.getMessage() : "<no msg>");
      Logger.println(msg);
      throw new Error(msg, thrown);
    } catch (Exception e) {
      Logger.printf("failed invoking %s.%s, %s\n", self.getClass().getName(), method.getName(), e.getMessage());
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

  protected int invokeIntMethod(String name, Object... parameters) {
    return invokeMethod(name, -1, parameters);
  }

  protected Object invokeObjectMethod(String name, Object... parameters) {
    return invokeMethod(name, null, parameters);
  }

  /**
   * Looks up a method signature by class names.
   * @param name Method name
   * @param parameterTypes Fully qualified class names of all parameters
   * @return Matching method if found. Otherwise, null.
   */
  protected Method getMethodByClassNames(String name, String... parameterTypes) {
    Method[] methods;

    try {
      methods = this.self.getClass().getMethods();
    } catch (Exception e) {
      Logger.printf("failed to get method %s: %s\n", name, e.getMessage());
      return null;
    }

    for (Method method : methods) {
      if (!method.getName().equals(name)) {
        continue;
      }

      final Class<?>[] methodParamTypes = method.getParameterTypes();
      if (methodParamTypes.length != parameterTypes.length) {
        continue;
      }

      boolean match = true;
      for (int i = 0; i < methodParamTypes.length; i++) {
        final String a = methodParamTypes[i].getName();
        final String b = parameterTypes[i];

        if (!a.equals(b)) {
          match = false;
          break;
        }
      }

      if (match) {
        return method;
      }
    }

    return null;
  }
}
