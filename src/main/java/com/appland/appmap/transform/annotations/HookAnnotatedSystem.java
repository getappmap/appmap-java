package com.appland.appmap.transform.annotations;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.util.StringUtil;
import javassist.CtBehavior;

public class HookAnnotatedSystem extends SourceMethodSystem {
  private String annotationClass;

  private HookAnnotatedSystem(CtBehavior behavior, String annotationClass) {
    super(behavior);

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
  public Boolean match(CtBehavior behavior) {
    final Boolean isExplicitlyExcluded = AppMapConfig.get().excludes(StringUtil.canonicalName(behavior));

    return behavior.hasAnnotation(this.annotationClass) && !isExplicitlyExcluded;
  }
}
