package com.appland.appmap.transform.annotations;

import java.util.Map;
import java.util.Set;

import com.appland.appmap.config.AppMapConfig;

import javassist.CtBehavior;

public class HookAnnotatedSystem extends SourceMethodSystem {
  private String annotationClass;

  private HookAnnotatedSystem(CtBehavior behavior, String annotationClass) {
    super(behavior, HookAnnotated.class);

    this.annotationClass = annotationClass;
  }

  /**
   * Factory method. Reads any relevant annotation information and caches it.
   * @param behavior The hook behavior
   * @return A new {@code HookAnnotatedSystem} if {@link HookAnnotated} is found. Otherwise,
   *         {@code null}.
   */
  public static ISystem from(CtBehavior behavior) {
    String annotatedClass = (String) AnnotationUtil.getValue(behavior, HookAnnotated.class, null);
    if (annotatedClass == null) {
      return null;
    }
    return new HookAnnotatedSystem(behavior, annotatedClass);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Boolean match(CtBehavior behavior, Map<String, Object> hookContext) {
    final Boolean isExplicitlyExcluded = AppMapConfig.get().excludes(behavior);

    Set<String> annotations = (Set<String>)hookContext.get("annotations");
    return !isExplicitlyExcluded && annotations != null && annotations.contains(annotationClass);
  }
}
