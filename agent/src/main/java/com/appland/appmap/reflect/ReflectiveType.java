package com.appland.appmap.reflect;

import com.appland.appmap.util.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectiveType {
  protected Object self;

  public ReflectiveType(Object self) {
    this.self = self;
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
    }
    catch (InvocationTargetException e) {
      Throwable thrown = e.getTargetException();
      final String msg = String.format("%s.%s threw an exception, %s\n", self.getClass().getName(), method.getName(), thrown != null? thrown.getMessage() : "<no msg>");
      Logger.println(msg);
      throw new Error(msg, thrown);
    }
    catch (Exception e) {
      Logger.printf("failed invoking %s.%s, %s\n", self.getClass().getName(), method.getName(), e.getMessage());
    }
    return null;
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
