package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.RecordCondition;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookCondition;
import com.appland.appmap.transform.annotations.MethodEvent;

public class RecordMethod {
  private static final Recorder recorder = Recorder.getInstance();

  @ArgumentArray
  @ExcludeReceiver
  @HookCondition(RecordCondition.class)
  public static void record(Event event, Object[] args) {
    RecordingSupport.startRecording(event, "record_process", "process");
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookCondition(value = RecordCondition.class, methodEvent = MethodEvent.METHOD_RETURN)
  public static void record(Event event, Object returnValue, Object[] args) {
    RecordingSupport.stopRecording(event);
  }

  @ArgumentArray
  @ExcludeReceiver
  @HookCondition(value = RecordCondition.class, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void record(Event event, Exception exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
    StackTraceElement ste = exception.getStackTrace()[0];
    RecordingSupport.stopRecording(new RecordingSupport.TestDetails(event), null, exception.getMessage(),
        ste.getLineNumber());
  }

}
