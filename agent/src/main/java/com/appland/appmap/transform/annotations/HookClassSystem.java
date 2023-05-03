package com.appland.appmap.transform.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.appland.appmap.util.Logger;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

public class HookClassSystem extends SourceMethodSystem {
  private final static Boolean IGNORE_CHILDREN_DEFAULT = false;

  private String targetClass = null;
  private String targetMethod = null;
  private Boolean ignoresChildren = IGNORE_CHILDREN_DEFAULT;
  private final Integer position;
  private final static Signatures SIGNATURE_DEFAULT = null;
  private List<Signature> signatures = null;

  private HookClassSystem(CtBehavior behavior, int position, List<Signature> signatures) {
    super(behavior, HookClass.class);
    this.position = position;
    this.signatures = signatures;
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

      Integer position = AnnotationUtil.getPosition(behavior, HookClass.class, ISystem.HOOK_POSITION_DEFAULT);

      Signature[] signatures = (Signature[]) AnnotationUtil.getObject(behavior, Signatures.class, "value",
          SIGNATURE_DEFAULT);
      Signature signature = (Signature) behavior.getAnnotation(Signature.class);
      List<Signature> types = null;
      if (signatures != null || signature != null) {
        types = new ArrayList<Signature>();
        if (signatures != null) {
          Collections.addAll(types, signatures);
        } else if (signature != null) {
          types.add(signature);
        }
      }

      HookClassSystem system = new HookClassSystem(behavior, position, types);
      system.ignoresChildren = ignoresChildren;
      system.targetClass = hookClass.value();
      system.targetMethod = hookClass.method() == null || hookClass.method().isEmpty()
          ? behavior.getName()
          : hookClass.method();

      return system;
    } catch (Exception e) {
      Logger.println(e);
      return null;
    }
  }

  @Override
  public Boolean match(CtBehavior behavior, Map<String, Object> matchResult) {
    String behaviorClass = behavior.getDeclaringClass().getName();

    if (this.ignoresChildren) {
      if (!behaviorClass.equals(this.targetClass)) {
        return false;
      }
    } else if (!CtClassUtil.isChildOf(behavior.getDeclaringClass(), this.targetClass)) {
      return false;
    }

    String behaviorName = behavior.getName();
    if (!behaviorName.equals(this.targetMethod)) {
      return false;
    }

    if (signatures != null) {
      try {
        for (Signature signature : signatures) {
          if (methodMatchesSignature(behavior, signature)) {
            return true;
          }
        }
        return false;
      } catch (NotFoundException e) {
        Logger.println("Failed to find type of parameters of " + behaviorClass + "."
            + behaviorName);
        Logger.println(e);
      }
    }

    return true;
  }

  @Override
  public String getKey() {
    return this.targetMethod;
  }

  @Override
  public Integer getHookPosition() {
    return position;
  }

  private static Boolean methodMatchesSignature(CtBehavior behavior, Signature signature) throws NotFoundException {
    // CtBehavior.getParameterTypes finds the class of the parameter by
    // name, and so can throw a NotFoundException.
    List<CtClass> behaviorTypes = Arrays.asList(behavior.getParameterTypes());
    String[] types = signature.value();
    if (behaviorTypes.size() != types.length) {
      return false;
    }
    for (int i = 0; i < types.length; i++) {
      if (!CtClassUtil.isChildOf(behaviorTypes.get(i), types[i])) {
        return false;
      }
    }

    return true;
  }
}
