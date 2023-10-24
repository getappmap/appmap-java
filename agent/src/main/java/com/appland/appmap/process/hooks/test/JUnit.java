package com.appland.appmap.process.hooks.test;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookAnnotated;
import com.appland.appmap.transform.annotations.MethodEvent;

public class JUnit {
  private static final Recorder recorder = Recorder.getInstance();
  static final String JUNIT_NAME = "junit";

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.Test")
  public static void junit(Event event, Object[] args) {
    Recorder.Metadata metadata = new Recorder.Metadata(JUNIT_NAME, TestSupport.TEST_RECORDER_TYPE);
    metadata.frameworks.add(new Recorder.Framework("JUnit", "4"));
    RecordingSupport.startRecording(event, metadata);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void junit(Event event, Object returnValue, Object[] args) {
    RecordingSupport.stopRecording(event, true);
  }

  @ArgumentArray
  @HookAnnotated(value = "org.junit.Test", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void junit(Event event, Object self, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    StackTraceElement ste = findErrorFrame(self, exception);
    RecordingSupport.stopRecording(new RecordingSupport.TestDetails(event), false, exception.getMessage(),
        ste.getLineNumber());
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated("org.junit.jupiter.api.Test")
  public static void junit5Test(Event event, Object[] args) {
    Recorder.Metadata metadata = new Recorder.Metadata(JUNIT_NAME, TestSupport.TEST_RECORDER_TYPE);
    metadata.frameworks.add(new Recorder.Framework("JUnit", "5"));
    RecordingSupport.startRecording(event, metadata);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookAnnotated(value = "org.junit.jupiter.api.Test", methodEvent = MethodEvent.METHOD_RETURN)
  public static void junit5Test(Event event, Object returnValue, Object[] args) {
    RecordingSupport.stopRecording(event, true);
  }

  @ArgumentArray
  @HookAnnotated(value = "org.junit.jupiter.api.Test", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void junit5Test(Event event, Object self, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    StackTraceElement errorFrame = findErrorFrame(self, exception);
    RecordingSupport.stopRecording(new RecordingSupport.TestDetails(event), false, exception.getMessage(),
        errorFrame.getLineNumber());
  }

  private static StackTraceElement findErrorFrame(Object self, Throwable exception) throws InternalError {
    String selfClass = self.getClass().getName();
    StackTraceElement errorFrame = null;
    for (StackTraceElement frame : exception.getStackTrace()) {
      if (frame.getClassName().equals(selfClass)) {
        errorFrame = frame;
        break;
      }
    }
    if (errorFrame == null) {
      throw new InternalError("no stack frame matched test class");
    }
    return errorFrame;
  }
}
