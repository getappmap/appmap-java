package javassist;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;

public class CtAppMapClassType {
  /**
   * Retrieve an annotation on a behavior by name of the annotation class.
   *
   * (This is a copy of CtClassType.getAnnotation(Class<?>), tweaked to take a
   * String instead of a Class. As far as I (ajp) can tell, there's no reason
   * this method was omitted from CtClassType -- maybe we should consider
   * contributing it upstream.)
   */
  public static Object getAnnotation(CtBehavior b, String typeName) throws ClassNotFoundException {
    MethodInfo mi = b.getMethodInfo2();
    AnnotationsAttribute ainfo = (AnnotationsAttribute)
      mi.getAttribute(AnnotationsAttribute.invisibleTag);
    AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
      mi.getAttribute(AnnotationsAttribute.visibleTag);
    return getAnnotationType(typeName,
                             b.getDeclaringClass().getClassPool(),
                             ainfo, ainfo2);
  }
    
    
  static Object getAnnotationType(String typeName, ClassPool cp,
                                  AnnotationsAttribute a1, AnnotationsAttribute a2)
    throws ClassNotFoundException
  {
    Annotation[] anno1, anno2;

    if (a1 == null)
      anno1 = null;
    else
      anno1 = a1.getAnnotations();

    if (a2 == null)
      anno2 = null;
    else
      anno2 = a2.getAnnotations();

    if (anno1 != null)
      for (int i = 0; i < anno1.length; i++)
        if (anno1[i].getTypeName().equals(typeName))
          return toAnnoType(anno1[i], cp);

    if (anno2 != null)
      for (int i = 0; i < anno2.length; i++)
        if (anno2[i].getTypeName().equals(typeName))
          return toAnnoType(anno2[i], cp);

    return null;
  }

  private static Object toAnnoType(Annotation anno, ClassPool cp)
    throws ClassNotFoundException
  {
    try {
      ClassLoader cl = cp.getClassLoader();
      return anno.toAnnotationType(cl, cp);
    }
    catch (ClassNotFoundException e) {
      ClassLoader cl2 = cp.getClass().getClassLoader();
      try {
        return anno.toAnnotationType(cl2, cp);
      }
      catch (ClassNotFoundException e2){
        try {
          Class<?> clazz = cp.get(anno.getTypeName()).toClass();
          return javassist.bytecode.annotation.AnnotationImpl.make(
                                                                   clazz.getClassLoader(),
                                                                   clazz, cp, anno);
        }
        catch (Throwable e3) {
          throw new ClassNotFoundException(anno.getTypeName());
        }
      }
    }
  }
}
