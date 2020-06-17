package com.appland.appmap.reflect;

import java.lang.reflect.Method;

import com.appland.appmap.util.Logger;

public class ReflectiveType {
  protected Object self;

  public ReflectiveType(Object self) {
    this.self = self;
  }

  public Object GetObject() {
    return this.self;
  }

  protected Method getMethod(String name, Class<?>... parameterTypes) {
    try {
      return this.self.getClass().getMethod(name, parameterTypes);
    } catch (Exception e) {
      Logger.printf("failed to get method %s: %s\n", name, e.getMessage());
      return null;
    }
  }

  /**
   * Looks up a method signature by class names.
   * @param name Method name
   * @param parameterTypes Fully qualified class names of all parameters
   * @return Matching method if found. Otherwise, null.
   */
  protected Method getMethod(String name, String... parameterTypes) {
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