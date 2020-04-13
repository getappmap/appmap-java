package com.appland.appmap.process.conditions;

import javassist.CtBehavior;

public interface Condition {
  public static Boolean match(CtBehavior behavior) {
    return false;
  }
}
