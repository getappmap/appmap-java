package com.appland.appmap.transform.metadata;

import com.appland.appmap.process.EventProcessorType;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

public class HookableClassName extends Hookable {
  private String className;

  public HookableClassName(String className, Hookable ... children) {
    super(children);
    this.className = className;
  }

  @Override
  protected Boolean match(CtClass classType) {
    if (classType.getName().equals(this.className)) {
      return true;
    }

    final ClassFile classFile = classType.getClassFile();
    if (classFile.getSuperclass().equals(this.className)) {
      return true;
    }

    return false;
  }
}