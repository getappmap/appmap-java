package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class HookableClassName extends Hookable {
  private String className;

  public HookableClassName(String className, Hookable ... children) {
    super(children);
    this.className = className;
  }

  @Override
  protected Boolean match(CtClass classType) {
    if (classType.getName().equals("org.kohsuke.stapler.Stapler")) {
      System.err.println("loaded");
    }
    
    if (classType.getName().equals(this.className)) {
      return true;
    }

    try {
      CtClass superClass = classType.getSuperclass();
      while (superClass != null) {
        if (superClass.getName().equals(this.className)) {
          return true;
        }
        superClass = superClass.getSuperclass();
      }
    } catch (NotFoundException e) {
      return false;
    }

    return false;
  }
}
