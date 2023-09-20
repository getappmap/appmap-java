package com.appland.appmap.process.hooks.test;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookAnnotated;
import com.appland.appmap.transform.annotations.MethodEvent;

public class TestNG {
  private static final String TESTNG_NAME = "testng";

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.testng.annotations.Test")
  public static void testng(Event event, Object[] args) {
    RecordingSupport.startRecording(event, TESTNG_NAME, TestSupport.TEST_RECORDER_TYPE);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.testng.annotations.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void testng(Event event, Object returnValue, Object[] args) {
    RecordingSupport.stopRecording(event, true);
  }

  // TODO: add support for recording TestNG test failures
  /*
   * @ArgumentArray
   * 
   * @ExcludeReceiver
   * 
   * @HookAnnotated(value = "org.testng.annotations.Test", methodEvent =
   * MethodEvent.METHOD_EXCEPTION)
   * public static void testng(Event event, Exception exception, Object[] args) {
   * event.setException(exception);
   * recorder.add(event);
   * // TODO: This is not always correct.
   * // https://www.javadoc.io/doc/org.testng/testng/6.9.4/org/testng/annotations/
   * Test.html
   * // allows for 'expectedExceptions' and 'expectedExceptionsMessageRegExp',
   * which
   * // allow a test to throw an exception without failing. This method does not
   * take
   * // that feature into account, so all test methods that throw exceptions will
   * be
   * // marked as failed.
   * RecordingSupport.stopRecording(event, false, exception);
   * }
   */

}
