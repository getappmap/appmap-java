package com.appland.appmap.transform.instrumentation;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import org.tinylog.TaggedLogger;

import com.appland.appmap.ClassFileExporter;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.transform.annotations.AppMapInstrumented;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;


public class BBTransformer implements AgentBuilder.Transformer {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  public static void installOn(Instrumentation inst) {
    AgentBuilder builder = new AgentBuilder.Default();
    if (Properties.SaveInstrumented) {
      Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "appmap", "bb");
      logger.info("Saving instrumented files to {}", dir);
      builder = builder.with(new ClassFileExporter(dir));
    }
    builder.with(ClassFileLocator.ForClassLoader.ofSystemLoader())
        .type(ElementMatchers.isAnnotatedWith(AppMapInstrumented.class))
        .transform(new BBTransformer())
        .installOn(inst);
  }

  private BBTransformer() {}

  @Override
  public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription,
      ClassLoader classLoader, JavaModule module, ProtectionDomain protectionDomain) {
    logger.trace("builder: {}, typeDescription: {}", builder, typeDescription);

    // Instrument the method using an ASMVisitor. This inserts the advice into
    // the instrumented method without adding any additional code.
    //
    // If we instead delegated to the advice class by calling builder.intercept,
    // bytebuddy rebases the instrumented method, creating a private synthetic
    // method. This new method is still visible to another
    // instrumentation/reflection framework (e.g. whatever flowable is using),
    // it may get confused.

    return builder
        .visit(BBAdvice.METHOD_INSTRUMENTOR)
        .visit(AnnotationRemover.FROM_APP_METHODS)
        .visit(AnnotationRemover.FROM_APP_CLASSES);
  }
}
