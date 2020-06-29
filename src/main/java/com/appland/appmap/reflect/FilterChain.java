package com.appland.appmap.reflect;

import java.lang.reflect.Method;

import com.appland.appmap.util.Logger;

public class FilterChain extends ReflectiveType {
  private static Method fnDoFilter;

  public FilterChain(Object self) {
    super(self);

    if (fnDoFilter == null) {
      fnDoFilter = this.getMethod("doFilter",
          "javax.servlet.ServletRequest",
          "javax.servlet.ServletResponse");
    }
  }

  public void doFilter(Object request, Object response) {
    if (fnDoFilter != null) {
      try {
        fnDoFilter.invoke(this.self, request, response);
      } catch (Exception e) {
        Logger.printf("failed to invoke method doFilter: %s", e.getMessage());
      }
    }
  }
}