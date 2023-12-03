package com.appland.appmap.process.hooks.test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.process.hooks.RecordingSupport.TestDetails;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.AnnotationUtil;

class TestSupport {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  static final String TEST_RECORDER_TYPE = "tests";

  static void startRecording(Event event, Recorder.Metadata metadata) {
    startRecording(new TestDetails(event), metadata, Thread.currentThread().getStackTrace());
  }

  static void startRecording(TestDetails details, Recorder.Metadata metadata) {
    startRecording(details, metadata, Thread.currentThread().getStackTrace());
  }

  private static void startRecording(TestDetails details, Recorder.Metadata metadata, StackTraceElement[] stack) {
    // stack[0] is the call to getStackTrace, stack[1] is the call to
    // startRecording, stack[2] is the call to the hook method, so stack[3] is
    // the call to the test method:
    logger.trace("stack: {}", () -> Arrays.stream(stack).map(StackTraceElement::toString)
        .collect(Collectors.joining("\n")));
    if (!isRecordingEnabled(stack[3])) {
      return;
    }

    RecordingSupport.startRecording(details, metadata);
  }

  private static boolean isRecordingEnabled(StackTraceElement ste) {
    Method testMethod = findTestMethod(ste);
    String noAppMap = "com.appland.appmap.annotation.NoAppMap";
    boolean methodAnnotated = AnnotationUtil.hasAnnotation(noAppMap, testMethod);
    boolean classAnnotated = AnnotationUtil.hasAnnotation(noAppMap, testMethod.getDeclaringClass());
    return !methodAnnotated && !classAnnotated;
  }

  /**
   * Find the test method that corresponds to the method named in the given StackTraceElement.
   *
   * This is complicated a little by the fact that method names in Java classes aren't unique, so
   * there may be more than one that matches. It's not common to have overloaded test methods,
   * though, so we'll just use the first one we find (and issue a warning if there are more).
   *
   * @param ste
   * @return
   */
  private static Method findTestMethod(StackTraceElement ste) {
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
