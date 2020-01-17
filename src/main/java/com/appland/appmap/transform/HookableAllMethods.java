package com.appland.appmap.transform;

import javassist.CtBehavior;
import javassist.Modifier;

public class HookableAllMethods extends Hookable {
  @Override
  protected Boolean match(CtBehavior behavior) {
    return Modifier.isPublic(behavior.getModifiers());
  }
}
