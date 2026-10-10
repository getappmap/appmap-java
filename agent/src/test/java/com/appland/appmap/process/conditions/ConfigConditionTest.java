package com.appland.appmap.process.conditions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.AppMapPackage;
import com.appland.appmap.test.util.ClassBuilder;
import com.appland.appmap.test.util.MethodBuilder;
import com.appland.appmap.util.AppMapClassPool;
import com.appland.appmap.util.LabelUtil;

import javassist.CtClass;

public class ConfigConditionTest {
  @RegisterExtension
  static final ClassBuilderResolver classBuilderResolver = new ClassBuilderResolver();

  @BeforeAll
  public static void beforeAll() {
    AppMapClassPool.acquire(Thread.currentThread().getContextClassLoader());
  }

  @AfterAll
  public static void afterAll() throws Exception {
    AppMapClassPool.release();
  }

  @FunctionalInterface
  interface BuilderConsumer {
    void accept(MethodBuilder methodBuilder) throws Exception;
  }

  // Ideally, testGetterMethods would have been written as
  //
  // @Test
  // @ValueSource(strings = {"get", "is", "has"})
  // @MethodSource("notGetters")
  // void testGetterMethods()
  //
  // i.e. each of the notGetter methods would have been tested with the each of
  // the prefixes.
  // JUnit doesn't support combining sources in this way, though, so using
  // dynamic tests seems to be the next best thing.
  @TestFactory
  Stream<DynamicNode> testGetterMethods() {
    return Stream.of("get", "is", "has")
        .map(prefix -> dynamicContainer(String.format("test %s", prefix), Stream.of(
            ignoredMethodTest(prefix),
            dynamicContainer("test not ignored methods", notIgnoredMethodTests(prefix)))));
  }

  DynamicNode ignoredMethodTest(String prefix) {
    return dynamicTest(String.format("test \"%s\" methods ignored", prefix), () -> {
      ClassBuilder classBuilder = new ClassBuilder("TestClass");
      MethodBuilder methodBuilder = classBuilder.beginMethod();
      // no params, returns value
      methodBuilder.setName(String.format("%sSomething", prefix))
          .setBody("return Integer.valueOf(1);")
          .setReturnType(CtClass.booleanType).endMethod();
      assertTrue(ConfigCondition.isGetter(methodBuilder.getBehavior()));
    });
  }

  Stream<DynamicNode> notIgnoredMethodTests(String prefix) {
    return notGetters().map((Arguments a) -> {
      Object[] args = a.get();
      String description = (String)args[0];
      return dynamicTest(String.format("not \"%s\" if %s", prefix, description), () -> {
        BuilderConsumer consumer = (BuilderConsumer)args[1];
        ClassBuilder classBuilder = new ClassBuilder("TestClass");
        MethodBuilder methodBuilder = classBuilder.beginMethod();
        methodBuilder.setName(prefix + "Something");
        try {
          consumer.accept(methodBuilder);
          assertFalse(ConfigCondition.isGetter(methodBuilder.getBehavior()));
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    });
  }

  // Each of elements return here describe a disqualifying feature for a method
  // that has a getter'ish name.
  Stream<Arguments> notGetters() {
    return Stream.of(
        Arguments.of("it returns void",
            (BuilderConsumer)methodBuilder -> methodBuilder
                .endMethod()),
        Arguments.of("it returns takes an argument",
            (BuilderConsumer)methodBuilder -> methodBuilder
                .addParameter("java.lang.Integer", "id")
                .setBody("return Integer.valueOf(1);")
                .setReturnType("java.lang.Integer").endMethod()));
  }

  @Test
  void testSetterMethod(ClassBuilder classBuilder) throws Exception {
    MethodBuilder methodBuilder = classBuilder.beginMethod();
    methodBuilder.setName("setSomething")
        .addParameter("java.lang.Integer", "id")
        .endMethod();
    assertTrue(ConfigCondition.isSetter(methodBuilder.getBehavior()));
  }

  @ParameterizedTest(name = "not a setter if {0}")
  @MethodSource("notSetters")
  void testNotSetterMethods(String name, BuilderConsumer consumer, ClassBuilder classBuilder)
      throws Exception {
    MethodBuilder methodBuilder = classBuilder.beginMethod();
    methodBuilder.setName("setSomething");
    consumer.accept(methodBuilder);
    assertFalse(ConfigCondition.isSetter(methodBuilder.getBehavior()));
  }

  static Stream<Arguments> notSetters() {
    return Stream.of(
        Arguments.of("it takes two arguments",
            (BuilderConsumer)methodBuilder -> methodBuilder
                .addParameter("java.lang.Integer", "key")
                .addParameter("java.lang.Integer", "value").endMethod()),
        Arguments.of("it returns something",
            (BuilderConsumer)methodBuilder -> methodBuilder
                .addParameter("java.lang.Integer", "value")
                .setBody("return null;")
                .setReturnType("java.lang.Integer").endMethod()));
  }

  @Nested
  class TrivialFilterBypass {
    private static final String PKG = "com.appland.testfixture";

    private final ConfigCondition condition = new ConfigCondition();
    private AppMapPackage[] originalPackages;
    private int classCounter;

    @BeforeEach
    public void saveConfig() {
      originalPackages = AppMapConfig.get().packages;
    }

    @AfterEach
    public void restoreConfig() {
      AppMapConfig.get().packages = originalPackages;
    }

    private MethodBuilder freshGetter(String methodName) {
      ClassBuilder cb = new ClassBuilder(PKG + ".Class" + (classCounter++));
      MethodBuilder mb = cb.beginMethod();
      mb.setName(methodName)
          .setBody("return \"x\";");
      try {
        mb.setReturnType("java.lang.String");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return mb;
    }

    private boolean matches(MethodBuilder mb) {
      Map<String, Object> result = new HashMap<>();
      return condition.match(mb.getBehavior(), result);
    }

    @Test
    public void plainGetterIsSkippedWithoutLabel() throws Exception {
      AppMapConfig.get().packages = new AppMapPackage[] {
          new AppMapPackage(PKG, null, false, null)
      };
      MethodBuilder mb = freshGetter("getValue");
      mb.endMethod();
      assertFalse(matches(mb));
    }

    @Test
    public void getterWithLabelsAnnotationIsRecorded() throws Exception {
      AppMapConfig.get().packages = new AppMapPackage[] {
          new AppMapPackage(PKG, null, false, null)
      };
      MethodBuilder mb = freshGetter("getSecret");
      mb.addAnnotation(LabelUtil.LABELS_CLASS).endMethod();
      assertTrue(matches(mb));
    }

    @Test
    public void setterWithLabelsAnnotationIsRecorded() throws Exception {
      AppMapConfig.get().packages = new AppMapPackage[] {
          new AppMapPackage(PKG, null, false, null)
      };
      ClassBuilder cb = new ClassBuilder(PKG + ".Class" + (classCounter++));
      MethodBuilder mb = cb.beginMethod();
      mb.setName("setSecret")
          .addParameter("java.lang.String", "value")
          .addAnnotation(LabelUtil.LABELS_CLASS)
          .endMethod();
      assertTrue(matches(mb));
    }

    @Test
    public void getterExplicitlyNamedInConfigIsRecorded() throws Exception {
      AppMapPackage.LabelConfig methodConfig = new AppMapPackage.LabelConfig(
          "Class.*", "getValue", new String[] {});
      AppMapConfig.get().packages = new AppMapPackage[] {
          new AppMapPackage(PKG, null, false, new AppMapPackage.LabelConfig[] { methodConfig })
      };
      MethodBuilder mb = freshGetter("getValue");
      mb.endMethod();
      assertTrue(matches(mb));
    }

    @Test
    public void getterWithLabelsAttachedInConfigIsRecorded() throws Exception {
      AppMapPackage.LabelConfig methodConfig = new AppMapPackage.LabelConfig(
          "Class.*", "getValue", new String[] { "secret" });
      AppMapConfig.get().packages = new AppMapPackage[] {
          new AppMapPackage(PKG, null, false, new AppMapPackage.LabelConfig[] { methodConfig })
      };
      MethodBuilder mb = freshGetter("getValue");
      mb.endMethod();
      assertTrue(matches(mb));
    }
  }

  static class ClassBuilderResolver implements ParameterResolver {
      @Override
      public boolean supportsParameter(ParameterContext parameterContext,
          ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == ClassBuilder.class;
      }

      @Override
      public Object resolveParameter(ParameterContext parameterContext,
          ExtensionContext extensionContext) throws ParameterResolutionException {
        return new ClassBuilder("TestClass");
      }
    }
  }



