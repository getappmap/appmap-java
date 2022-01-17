package com.appland.appmap.util;

import java.lang.reflect.Modifier;

import javassist.CtBehavior;


import com.appland.appmap.config.Properties;

public class AppMapBehavior {
  private final CtBehavior behavior_;

  public AppMapBehavior(CtBehavior behavior) {
    behavior_ = behavior;
  }

  public Boolean isRecordable() {
    return !Modifier.isPrivate(behavior_.getModifiers()) || Properties.RecordPrivate;
  }
}
