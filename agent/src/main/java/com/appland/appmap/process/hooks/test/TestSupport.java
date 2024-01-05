package com.appland.appmap.process.hooks.test;

import static com.appland.appmap.transform.annotations.AnnotationUtil.hasAnnotation;
import static com.appland.appmap.util.ClassUtil.safeClassForName;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.process.hooks.RecordingSupport.TestDetails;
import com.appland.appmap.record.Recorder;

class TestSupport {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static final String PACKAGE_NAME = TestSupport.class.getPackage().getName();

  static final String TEST_RECORDER_TYPE = "tests";

  static void startRecording(Event event, Recorder.Metadata metadata) {
    startRecording(new TestDetails(event), metadata, Thread.currentThread().getStackTrace());
  }

  static void startRecording(TestDetails details, Recorder.Metadata metadata) {
    startRecording(details, metadata, Thread.currentThread().getStackTrace());
  }

  private static void startRecording(TestDetails details, Recorder.Metadata metadata,
      StackTraceElement[] stack) {
    logger.trace("stack: {}", () -> Arrays.stream(stack).map(StackTraceElement::toString)
        .collect(Collectors.joining("\n")));

    // Walk up the stack until we find a method with a @Test annotation.
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Method testMethod = null;
    Class<?> jupiterTest = safeClassForName(cl, "org.junit.jupiter.api.Test");
    Class<?> junitTest = safeClassForName(cl, "org.junit.Test");
    for (int idx = 0; idx < stack.length; idx++) {
      String className = stack[idx].getClassName();
      if (className.startsWith("java.lang")
          || className.startsWith(PACKAGE_NAME)) {
        continue;
      }
      Method stackMethod = findStackMethod(stack[idx]);
      if ((jupiterTest != null && hasAnnotation(jupiterTest, stackMethod)
          || junitTest != null && hasAnnotation(junitTest, stackMethod))) {
        testMethod = stackMethod;
        break;
      }
    }
    if (testMethod == null) {
      logger.warn("Couldn't find a test method on the stack:\n {}",
          () -> Arrays.stream(stack).map(StackTraceElement::toString)
              .collect(Collectors.joining("\n")));
      throw new InternalError("Couldn't find a test method on the stack");
    }

    if (!isRecordingEnabled(cl, testMethod)) {
      return;
    }

    RecordingSupport.startRecording(details, metadata);
  }

  private static boolean isRecordingEnabled(ClassLoader cl, Method testMethod) {
    Class<?> noAppMap = safeClassForName(cl, "com.appland.appmap.annotation.NoAppMap");
    boolean methodAnnotated = hasAnnotation(noAppMap, testMethod);
    boolean classAnnotated = hasAnnotation(noAppMap, testMethod.getDeclaringClass());
    return !methodAnnotated && !classAnnotated;
  }

  /**
   * Find the Method that corresponds to the method named in the given StackTraceElement.
   *
   * This is complicated a little by the fact that method names in Java classes aren't unique, so
   * there may be more than one that matches. It's not common to have overloaded test methods,
   * though, so we'll just use the first one we find (and issue a warning if there are more).
   *
   * @param ste the StackTraceElement to examine
   * @return
   */
  private static Method findStackMethod(StackTraceElement ste) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      String className = ste.getClassName();
      String methodName = ste.getMethodName();

      Class<?> cls = Class.forName(className, true, cl);
      Method[] methods = Arrays.stream(cls.getDeclaredMethods())
          .filter(m -> m.getName().equals(methodName)).toArray(Method[]::new);
      if (methods.length == 0) {
        throw new InternalError("No method named " + methodName + " in " + className);
      }
      Method method = methods[0];
      if (methods.length > 1) {
        logger.warn("Found {} methods named {} in {}, using {}", methods.length, methodName,
            className, method.getName());
      }
      return method;
    } catch (SecurityException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
