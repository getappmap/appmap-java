package com.appland.appmap.process.hooks.remoterecording;

import static com.appland.appmap.util.StringUtil.canonicalName;
import static com.appland.appmap.util.StringUtil.decapitalize;
import static com.appland.appmap.util.StringUtil.identifierToSentence;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.RecordCondition;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookAnnotated;
import com.appland.appmap.transform.annotations.HookCondition;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.util.Logger;

/*
  * TODO: move the test framework hooks (JUnit, TestNG) somewhere else.
  */

public class ServletHooks {
  private static final Recorder recorder = Recorder.getInstance();

  private static final String JUNIT_NAME = "junit";
  private static final String TESTNG_NAME = "testng";
  private static final String TEST_RECORDER_TYPE = "tests";

  private static void startRecording(Event event, String recorderName, String recorderType) {
    Logger.printf("Recording started for %s\n", canonicalName(event));
    try {
      Recorder.Metadata metadata = new Recorder.Metadata(recorderName, recorderType);
      final String feature = identifierToSentence(event.methodId);
      final String featureGroup = identifierToSentence(event.definedClass);
      metadata.scenarioName = String.format(
          "%s %s",
          featureGroup,
          decapitalize(feature));
      metadata.recordedClassName = event.definedClass;
      metadata.recordedMethodName = event.methodId;
      metadata.sourceLocation = String.join(":", new String[]{ event.path, String.valueOf(event.lineNumber) });
      recorder.start(metadata);
    } catch (ActiveSessionException e) {
      Logger.printf("%s\n", e.getMessage());
    }
  }

  private static void stopRecording(Event event) {
    stopRecording(event, null, null);
  }

  private static void stopRecording(Event event, boolean succeeded) {
    stopRecording(event, succeeded, null);
  }

  private static void stopRecording(Event event, Boolean succeeded, Throwable exception) {
    Logger.printf("Recording stopped for %s\n", canonicalName(event));
    String filePath = Recorder.sanitizeFilename(String.join("_", event.definedClass, event.methodId));
    filePath += ".appmap.json";
    if ( succeeded != null ) {
      recorder.getMetadata().testSucceeded = succeeded;
    }
    if ( exception != null  ) {
      recorder.getMetadata().exception = exception;
    }
    Recording recording = recorder.stop();
    recording.moveTo(filePath);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void junit(Event event, Object[] args) {
    startRecording(event, JUNIT_NAME, TEST_RECORDER_TYPE);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void junit(Event event, Object returnValue, Object[] args) {
    stopRecording(event, true);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.Test", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void junit(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    stopRecording(event, false, exception);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.jupiter.api.Test")
  public static void junitJupiter(Event event, Object[] args) {
    startRecording(event, JUNIT_NAME, TEST_RECORDER_TYPE);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.jupiter.api.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void junitJupiter(Event event, Object returnValue, Object[] args) {
    stopRecording(event, true);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.jupiter.api.Test", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void junitJupiter(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    stopRecording(event, false, exception);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.testng.annotations.Test")
  public static void testng(Event event, Object[] args) {
    startRecording(event, TESTNG_NAME, TEST_RECORDER_TYPE);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.testng.annotations.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void testng(Event event, Object returnValue, Object[] args) {
    stopRecording(event, true);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.testng.annotations.Test", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void testng(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    // TODO: This is not always correct.
    // https://www.javadoc.io/doc/org.testng/testng/6.9.4/org/testng/annotations/Test.html
    // allows for 'expectedExceptions' and 'expectedExceptionsMessageRegExp', which
    // allow a test to throw an exception without failing. This method does not take
    // that feature into account, so all test methods that throw exceptions will be
    // marked as failed.
    stopRecording(event, false, exception);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookCondition(RecordCondition.class)
  public static void record(Event event, Object[] args) {
    startRecording(event, "record_process", "process");
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookCondition(value = RecordCondition.class, methodEvent = MethodEvent.METHOD_RETURN)
  public static void record(Event event, Object returnValue, Object[] args) {
    stopRecording(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookCondition(value = RecordCondition.class, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void record(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    stopRecording(event, null, exception);
  }
}
