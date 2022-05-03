package com.appland.appmap.process.conditions;

import java.util.Map;
import javassist.CtBehavior;

public interface Condition {
  public static Boolean match(CtBehavior behavior, Map<String, Object> matchResult) {
    return false;
  }
}
