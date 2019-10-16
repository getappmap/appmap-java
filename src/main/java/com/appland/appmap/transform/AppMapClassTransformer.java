package com.appland.appmap.transform;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

public class AppMapClassTransformer implements ClassFileTransformer {
  private ArrayList<SelectiveClassFileTransformer> subTransforms =
      new ArrayList<SelectiveClassFileTransformer>();

  public AppMapClassTransformer() {
    super();
  }

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class redefiningClass,
                          ProtectionDomain domain,
                          byte[] bytes) throws IllegalClassFormatException {
    ClassPool classPool = new ClassPool();
    classPool.appendClassPath(new LoaderClassPath(loader));

    try {
      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(bytes));
      if (ctClass.isInterface()) {
        return bytes;
      }

      for (SelectiveClassFileTransformer subTransform : subTransforms) {
        if (subTransform.canTransformClass(ctClass)) {
          return subTransform.transform(loader, ctClass);
        }
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (CannotCompileException e) {
      System.err.println(e.getMessage());
    }

    return bytes;
  }

  public AppMapClassTransformer addSubTransform(SelectiveClassFileTransformer subTransform) {
    this.subTransforms.add(subTransform);
    return this;
  }
}
