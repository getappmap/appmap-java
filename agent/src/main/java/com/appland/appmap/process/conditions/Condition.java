package com.appland.appmap.process.conditions;

import java.util.Map;

import javassist.CtBehavior;

public interface Condition {
  Boolean match(CtBehavior behavior, Map<String, Object> matchResult);
}
