package com.appland.appmap.process.hooks.test;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookAnnotated;
import com.appland.appmap.transform.annotations.MethodEvent;

public class JUnit4 {
  private static final Recorder recorder = Recorder.getInstance();

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void junit(Event event, Object[] args) {
    RecordingSupport.startRecording(event, JUnit5.JUNIT_NAME, TestSupport.TEST_RECORDER_TYPE);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void junit(Event event, Object returnValue, Object[] args) {
    RecordingSupport.stopRecording(event, true);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.Test", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void junit(Event event, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    StackTraceElement ste = exception.getStackTrace()[0];
    RecordingSupport.stopRecording(new RecordingSupport.TestDetails(event), null, exception.getMessage(),
        ste.getLineNumber());
  }

}
