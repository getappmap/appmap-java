package com.appland.appmap.transform.instrumentation;

import com.appland.appmap.transform.annotations.AppMapAppMethod;
import com.appland.appmap.transform.annotations.AppMapInstrumented;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

class AnnotationRemover {
  static final AsmVisitorWrapper FROM_APP_METHODS = new AsmVisitorWrapper.ForDeclaredMethods()
      .method(ElementMatchers.isAnnotatedWith(AppMapAppMethod.class),
          (instrumentedType, instrumentedMethod, methodVisitor, implementationContext,
              typePool,
              writerFlags, readerFlags) -> {
            return new MethodVisitor(Opcodes.ASM7, methodVisitor) {
              @Override
              public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                String descriptor =
                    TypeDescription.ForLoadedType.of(AppMapAppMethod.class).getDescriptor();
                if (desc.equals(descriptor)) {
                  // Skip the annotation
                  return null;
                }
                return super.visitAnnotation(desc, visible);
              }
            };
          });

  static final AsmVisitorWrapper FROM_APP_CLASSES = new AsmVisitorWrapper() {
    @Override
    public int mergeWriter(int flags) {
      return flags;
    }

    @Override
    public int mergeReader(int flags) {
      return flags;
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType,
        ClassVisitor classVisitor, Context implementationContext, TypePool typePool,
        FieldList<net.bytebuddy.description.field.FieldDescription.InDefinedShape> fields,
        MethodList<?> methods, int writerFlags, int readerFlags) {
      return new ClassVisitor(Opcodes.ASM7, classVisitor) {
        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
          String descriptor =
              TypeDescription.ForLoadedType.of(AppMapInstrumented.class).getDescriptor();
          if (desc.equals(descriptor)) {
            // Skip the annotation
            return null;
          }
          return super.visitAnnotation(desc, visible);
        }
      };
    }
  };
}
