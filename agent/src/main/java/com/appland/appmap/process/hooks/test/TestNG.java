package com.appland.appmap.process.hooks.test;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.reflect.ReflectiveType;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookAnnotated;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;

public class TestNG {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static final String TESTNG_NAME = "testng";

  private static final Recorder recorder = Recorder.getInstance();
  private static final ThreadLocal<Event> lastReturnEvent = new ThreadLocal<>();

  private static class ITestResult extends ReflectiveType {
    private static final String GET_INSTANCE = "getInstance";
    private static String GET_THROWABLE = "getThrowable";

    public ITestResult(Object self) {
      super(self);
      addMethods(GET_INSTANCE, GET_THROWABLE);
    }

    public Object getInstance() {
      return invokeObjectMethod(GET_INSTANCE);
    }

    public Throwable getThrowable() {
      return (Throwable)invokeObjectMethod(GET_THROWABLE);
    }
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.testng.annotations.Test")
  public static void testng(Event event, Object[] args) {
    TestSupport.startRecording(event,
        new Recorder.Metadata(TESTNG_NAME, TestSupport.TEST_RECORDER_TYPE));
    lastReturnEvent.set(null);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.testng.annotations.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void testng(Event event, Object returnValue, Object[] args) {
    lastReturnEvent.set(event);
  }

  @ArgumentArray
  @HookAnnotated(value = "org.testng.annotations.Test", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void testng(Event event, Object self, Throwable exception, Object[] args) {
    lastReturnEvent.set(event);
  }

  /**
   * TestNG installs an ExitCodeListener to specify callbacks for events related to a test method.
   * We're not interested in the calls to the callbacks, only the test results that get passed to
   * them.
   **/

  /**
   * ExitCodeListener.onTestSuccess is called by TestNG when a test succeeds. This includes the case
   * that the test threw an expected exception.
   *
   * @param listenerEvent the call event for onTestSuccess, ignored
   * @param listenerArgs ignored
   */
  @ArgumentArray
  @ExcludeReceiver
  @HookClass("org.testng.internal.ExitCodeListener")
  public static void onTestSuccess(Event listenerEvent, Object[] listenerArgs) {
    Event event = lastReturnEvent.get();
    logger.debug("retEvent: {}", event);
    if (event == null) {
      logger.warn(new Exception(), "no return event set for thread {}",
          Thread.currentThread().getName());
      return;
    }
    RecordingSupport.stopRecording(event, true);
  }

  /**
   * ExitCodeListener.onTestFailure is called by TestNG when a test fails by throwing an exception.
   * This includes, but isn't limited to, an assertion failure (which throws AssertionError).
   *
   * @param listenerEvent the call event for onTestFailure, ignored
   * @param listenerArgs args[0] is the ITestResult describing the failure
   */
  @ArgumentArray
  @HookClass("org.testng.internal.ExitCodeListener")
  public static void onTestFailure(Event listenerEvent, Object listener, Object[] listenerArgs) {
    Event event = lastReturnEvent.get();
    logger.debug("retEvent: {}", event);
    if (event == null) {
      logger.warn(new Exception(), "no return event set for thread {}",
          Thread.currentThread().getName());
      return;
    }

    ITestResult testResult = new ITestResult(listenerArgs[0]);
    Object self = testResult.getInstance();
    Throwable exception = testResult.getThrowable();
    event.setException(exception);
    recorder.add(event);

    StackTraceElement ste = TestSupport.findErrorFrame(self, exception);
    RecordingSupport.stopRecording(new RecordingSupport.TestDetails(event), false,
        exception.getMessage(), ste.getLineNumber());
  }
}
