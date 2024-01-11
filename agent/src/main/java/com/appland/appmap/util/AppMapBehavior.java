package com.appland.appmap.util;

import java.lang.reflect.Modifier;

import com.appland.appmap.config.Properties;

import javassist.CtBehavior;

public class AppMapBehavior {
  public static Boolean isRecordable(CtBehavior behavior) {
    return !Modifier.isPrivate(behavior.getModifiers()) || Properties.RecordPrivate;
  }
}
