package com.appland.appmap.process.hooks.test;

import java.lang.reflect.Method;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.process.hooks.RecordingSupport.TestDetails;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.AnnotationUtil;
import com.appland.appmap.util.ClassUtil;

class TestSupport {
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
    if (!isRecordingEnabled(stack[3])) {
      return;
    }

    RecordingSupport.startRecording(details, metadata);
  }

  private static boolean isRecordingEnabled(StackTraceElement ste) {
    Method testMethod = ClassUtil.findMethod(ste);
    String noAppMap = "com.appland.appmap.annotation.NoAppMap";
    boolean methodAnnotated = AnnotationUtil.hasAnnotation(noAppMap, testMethod);
    boolean classAnnotated = AnnotationUtil.hasAnnotation(noAppMap, testMethod.getDeclaringClass());
    return !methodAnnotated && !classAnnotated;
  }
}
