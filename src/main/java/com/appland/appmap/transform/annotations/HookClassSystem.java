package com.appland.appmap.transform.annotations;

import javassist.CtBehavior;

public class HookClassSystem extends SourceMethodSystem {
  private final static Boolean IGNORE_CHILDREN_DEFAULT = false;

  private String targetClass = null;
  private String targetMethod = null;
  private Boolean ignoresChildren = IGNORE_CHILDREN_DEFAULT;

  private HookClassSystem(CtBehavior behavior) {
    super(behavior);
  }

  /**
   * Factory method. Reads any relevant annotation information and caches it.
   * @param behavior The hook behavior
   * @return A new {@code HookClassSystem} if {@link HookClass} is found. Otherwise, {@code null}.
   */
  public static ISystem from(CtBehavior behavior) {
    try {
      HookClass hookClass = (HookClass) behavior.getAnnotation(HookClass.class);
      if (hookClass == null) {
        hookClass = (HookClass) behavior.getDeclaringClass().getAnnotation(HookClass.class);
      }

      if (hookClass == null) {
        return null;
      }

      if (hookClass.value() == null) {
        return null;
      }

      Boolean ignoresChildren = (Boolean) AnnotationUtil.getValue(behavior,
          IgnoreChildren.class,
          IGNORE_CHILDREN_DEFAULT);

      HookClassSystem system = new HookClassSystem(behavior);
      system.ignoresChildren = ignoresChildren;
      system.targetClass = hookClass.value();
      system.targetMethod = hookClass.method() == null || hookClass.method().isEmpty() 
          ? behavior.getName()
          : hookClass.method();

      return system;
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  @Override
  public Boolean match(CtBehavior behavior) {
    if (this.ignoresChildren) {
      if (!behavior.getDeclaringClass().getName().equals(this.targetClass)) {
        return false;
      }
    } else if (!CtClassUtil.isChildOf(behavior.getDeclaringClass(), this.targetClass)) {
      return false;
    }

    if (!behavior.getName().equals(this.targetMethod)) {
      return false;
    }

    return true;
  }

  @Override
  public String getKey() {
    return this.targetMethod;
  }
}
