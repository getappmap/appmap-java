package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class HookableAnnotated extends Hookable {
  private String annotationName;

  public HookableAnnotated(String annotationName) {
    this.annotationName = annotationName;
  }

  @Override
  protected Boolean match(CtBehavior behavior) {
    return behavior.hasAnnotation(this.annotationName);
  }
}
