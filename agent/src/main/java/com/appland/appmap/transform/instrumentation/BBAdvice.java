package com.appland.appmap.transform.instrumentation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import com.appland.appmap.runtime.HookFunctions;
import com.appland.appmap.transform.annotations.AppMapAppMethod;
import com.appland.appmap.transform.annotations.MethodEvent;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.AllArguments;
import net.bytebuddy.asm.Advice.OffsetMapping;
import net.bytebuddy.asm.Advice.OffsetMapping.Factory.AdviceType;
import net.bytebuddy.asm.Advice.OnMethodEnter;
import net.bytebuddy.asm.Advice.OnMethodExit;
import net.bytebuddy.asm.Advice.Origin;
import net.bytebuddy.asm.Advice.Return;
import net.bytebuddy.asm.Advice.This;
import net.bytebuddy.asm.Advice.Thrown;
import net.bytebuddy.asm.AsmVisitorWrapper.ForDeclaredMethods;
import net.bytebuddy.description.annotation.AnnotationDescription.Loadable;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.matcher.ElementMatchers;

public class BBAdvice {
  static final ForDeclaredMethods METHOD_INSTRUMENTOR =
      Advice.withCustomMapping()
          .bind(CallOrdinalFactory.INSTANCE)
          .bind(ReturnOrdinalFactory.INSTANCE)
          .bind(ExcOrdinalFactory.INSTANCE)
          .to(BBAdvice.class)
          .on(ElementMatchers.isAnnotatedWith(AppMapAppMethod.class));


  // Define an annotation for each type of event template ordinal. Byte Buddy
  // doesn't support binding an annotation value of type int[], so we have to do
  // this instead.
  @Retention(RetentionPolicy.RUNTIME)
  private @interface CallOrdinal {
  }
  @Retention(RetentionPolicy.RUNTIME)
  private @interface ReturnOrdinal {
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface ExcOrdinal {
  }

  abstract private static class OrdinalFactory {
    protected OffsetMapping make(InDefinedShape target, Loadable<?> annotation,
        AdviceType adviceType, int ordinalIndex) {
      return new Advice.OffsetMapping() {
        @Override
        public Target resolve(TypeDescription instrumentedType,
            MethodDescription instrumentedMethod, Assigner assigner,
            Advice.ArgumentHandler argumentHandler, Sort sort) {
          int[] ordinals = (int[])instrumentedMethod.getDeclaredAnnotations()
              .ofType(AppMapAppMethod.class)
              .getValue("value")
              .resolve();
          return Target.ForStackManipulation.of(ordinals[ordinalIndex]);
        }
      };
    }
  }

  private static class CallOrdinalFactory extends OrdinalFactory
      implements OffsetMapping.Factory<CallOrdinal> {
    public static final CallOrdinalFactory INSTANCE = new CallOrdinalFactory();

    @Override
    public Class<CallOrdinal> getAnnotationType() {
      return CallOrdinal.class;
    }

    @Override
    public OffsetMapping make(InDefinedShape target, Loadable<CallOrdinal> annotation,
        AdviceType adviceType) {
      return super.make(target, annotation, adviceType, MethodEvent.METHOD_INVOCATION.getIndex());
    }
  }

  private static class ReturnOrdinalFactory extends OrdinalFactory
      implements OffsetMapping.Factory<ReturnOrdinal> {
    public static final ReturnOrdinalFactory INSTANCE = new ReturnOrdinalFactory();

    @Override
    public Class<ReturnOrdinal> getAnnotationType() {
      return ReturnOrdinal.class;
    }

    @Override
    public OffsetMapping make(InDefinedShape target, Loadable<ReturnOrdinal> annotation,
        AdviceType adviceType) {
      return super.make(target, annotation, adviceType, MethodEvent.METHOD_RETURN.getIndex());
    }
  }

  private static class ExcOrdinalFactory extends OrdinalFactory
      implements OffsetMapping.Factory<ExcOrdinal> {
    public static final ExcOrdinalFactory INSTANCE = new ExcOrdinalFactory();

    @Override
    public Class<ExcOrdinal> getAnnotationType() {
      return ExcOrdinal.class;
    }

    @Override
    public OffsetMapping make(InDefinedShape target, Loadable<ExcOrdinal> annotation,
        AdviceType adviceType) {
      return super.make(target, annotation, adviceType, MethodEvent.METHOD_EXCEPTION.getIndex());
    }
  }


  // onEnter and onExit aren't called directly. Instead, we use Byte Buddy to
  // embed their bytecode into instrumented methods. This approach imposes the
  // requirement that they, as well as any methods they call, be public.
  @OnMethodEnter
  public static void onEnter(@CallOrdinal int callOrdinal,
      @This(optional = true) Object self,
      @AllArguments Object[] args) throws Throwable {
    HookFunctions.onMethodCall.accept(callOrdinal, self, args);
  }

  @OnMethodExit(onThrowable = Throwable.class)
  public static void onExit(@ReturnOrdinal int returnOrdinal, @ExcOrdinal int excOrdinal,
      @This(optional = true) Object self,
      @Origin Method method,
      @AllArguments Object[] args, @Return(typing = Typing.DYNAMIC) Object ret,
      @Thrown Throwable exc) throws Throwable {
    HookFunctions.onMethodReturn.accept(returnOrdinal, excOrdinal, self, method, args, ret, exc);
  }
}
