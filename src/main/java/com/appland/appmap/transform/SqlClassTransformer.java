package com.appland.appmap.transform;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import javassist.CtBehavior;
import javassist.CtClass;

public class SqlClassTransformer extends SelectiveClassFileTransformer {
  public SqlClassTransformer() {
    super();
  }

  @Override
  public Boolean canTransformClass(CtClass classType) {
    return false;
  }

  @Override
  public Boolean canTransformBehavior(CtBehavior behavior) {
    return false;
  }
}
