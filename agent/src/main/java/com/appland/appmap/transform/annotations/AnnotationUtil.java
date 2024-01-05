package com.appland.appmap.transform.annotations;

import static com.appland.appmap.util.ClassUtil.safeClassForName;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;

/**
 * Utility classes for accessing runtime Annotation information.
 */
public class AnnotationUtil {
  public interface Annotated {
    ClassFile getClassFile();

    AnnotationsAttribute get();

    void setAnnotations(AnnotationsAttribute attr);
  }
  public static class AnnotatedBehavior implements Annotated {
    private final MethodInfo methodInfo;
    final CtBehavior behavior;

    public AnnotatedBehavior(CtBehavior behavior) {
      this.behavior = behavior;
      this.methodInfo = behavior.getMethodInfo();
    }

    @Override
    public AnnotationsAttribute get() {
      return (AnnotationsAttribute)methodInfo.getAttribute(AnnotationsAttribute.visibleTag);
    }

    @Override
    public void setAnnotations(AnnotationsAttribute attr) {
      methodInfo.addAttribute(attr);
    }

    @Override
    public ClassFile getClassFile() {
      return behavior.getDeclaringClass().getClassFile();
    }
  }
  public static class AnnotatedClass implements Annotated {
    private final ClassFile classFile;

    public AnnotatedClass(CtClass cls) {
      classFile = cls.getClassFile();
    }

    @Override
    public AnnotationsAttribute get() {
      return (AnnotationsAttribute)classFile.getAttribute(AnnotationsAttribute.visibleTag);
    }

    @Override
    public void setAnnotations(AnnotationsAttribute attr) {
      classFile.addAttribute(attr);
    }

    @Override
    public ClassFile getClassFile() {
      return classFile;
    }
  }

  /**
   * Obtains the default {@code value()} from an Annotation.
   * @param behavior The declaring behavior
   * @param annotationClass The annotation class
   * @param defaultValue A default value to use if an error occurs
   * @return The resulting value of the Annotations {@code value()} method or the default value
   *         supplied.
   */
  public static Object getValue(CtBehavior behavior,
                                Class<?> annotationClass,
                                Object defaultValue) {
    return getObject(behavior, annotationClass, "value", defaultValue);
  }

  public static Integer getPosition(CtBehavior behavior,
                                Class<?> annotationClass,
                                Object defaultValue) {
    return (Integer)getObject(behavior, annotationClass, "position", defaultValue);
  }

  public static Object getObject(CtBehavior behavior,
                                Class<?> annotationClass,
                                String annotationName,
                                Object defaultValue) {
    try {
      Object annotation = behavior.getAnnotation(annotationClass);
      if (annotation == null) {
        annotation = behavior.getDeclaringClass().getAnnotation(annotationClass);
      }

      if (annotation == null) {
        return defaultValue;
      }

      Method valueMethod = annotationClass.getMethod(annotationName);
      if (valueMethod == null) {
        return defaultValue;
      }

      return valueMethod.invoke(annotation);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static boolean hasAnnotation(String annotationName, AnnotatedElement elt) {
    return hasAnnotation(safeClassForName(AnnotationUtil.class.getClassLoader(), annotationName),
        elt);
  }

  @SuppressWarnings("unchecked")
  public static boolean hasAnnotation(Class<?> annotation, AnnotatedElement elt) {
    return annotation != null
        && elt.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>)annotation);
  }


  /**
   * Create a new annotation on the given method of the given type.
   *
   * @param behavior the method to annotate
   * @param annotationClass the class of the annotation
   * @return the new annotation
   */
  public static Annotation newBehaviorAnnotation(AnnotatedBehavior ab, Class<?> annotationClass) {
    CtClass ctClass = ab.behavior.getDeclaringClass();
    ClassFile classFile = ctClass.getClassFile();
    ConstPool constPool = classFile.getConstPool();

    return new Annotation(annotationClass.getName(), constPool);
  }

  public static void setAnnotation(AnnotatedBehavior behavior, Class<?> annotationClass) {
    setAnnotation(behavior, newBehaviorAnnotation(behavior, annotationClass));
  }

  /**
   * For the given method, set the "value" of the given annotation. Updates the annotation if it
   * exists, adds it if doesn't.
   *
   * @param methodInfo the method to annotate
   * @param annotation the annotation to add/update
   */
  public static void setAnnotation(Annotated annotated, Annotation annotation) {
    ClassFile classFile = annotated.getClassFile();
    ConstPool constPool = classFile.getConstPool();

    // MethodInfo methodInfo = behavior.getMethodInfo();
    // AnnotationsAttribute attr =
    // (AnnotationsAttribute)methodInfo.getAttribute(AnnotationsAttribute.visibleTag);

    AnnotationsAttribute attr = annotated.get();
    if (attr == null) {
      attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
    }
    Annotation[] existingAnnotations = attr.getAnnotations();

    boolean foundExisting = false;
    for (Annotation a : existingAnnotations) {
      if (a.getTypeName().equals(annotation.getTypeName())) {
        MemberValue memberValue = annotation.getMemberValue("value");
        if (memberValue != null) {
          a.addMemberValue("value", memberValue);
        }
        foundExisting = true;
        break;
      }
    }

    if (!foundExisting) {
      Annotation newAnnotation = new Annotation(annotation.getTypeName(), constPool);
      MemberValue memberValue = annotation.getMemberValue("value");
      if (memberValue != null) {
        newAnnotation.addMemberValue("value", memberValue);
      }
      attr.addAnnotation(newAnnotation);
    } else {
      attr.setAnnotations(existingAnnotations);
    }

    annotated.setAnnotations(attr);
  }
}
