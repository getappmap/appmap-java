package com.appland.appmap.process.hooks;

import static com.appland.appmap.util.StringUtil.canonicalName;
import static com.appland.appmap.util.StringUtil.decapitalize;
import static com.appland.appmap.util.StringUtil.identifierToSentence;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.util.Logger;

public class RecordingSupport {
  private static final Recorder recorder = Recorder.getInstance();

  public static void startRecording(Event event, String recorderName, String recorderType) {
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
      metadata.sourceLocation = String.join(":", new String[] { event.path, String.valueOf(event.lineNumber) });
      recorder.start(metadata);
    } catch (ActiveSessionException e) {
      Logger.printf("%s\n", e.getMessage());
    }
  }

  public static void stopRecording(Event event) {
    RecordingSupport.stopRecording(event, null, null);
  }

  public static void stopRecording(Event event, boolean succeeded) {
    RecordingSupport.stopRecording(event, succeeded, null);
  }

  public static void stopRecording(Event event, Boolean succeeded, Throwable exception) {
    Logger.printf("Recording stopped for %s\n", canonicalName(event));
    String filePath = Recorder.sanitizeFilename(String.join("_", event.definedClass, event.methodId));
    filePath += ".appmap.json";
    if (succeeded != null) {
      recorder.getMetadata().testSucceeded = succeeded;
    }
    if (exception != null) {
      recorder.getMetadata().exception = exception;
    }
    Recording recording = recorder.stop();
    recording.moveTo(filePath);
  }
}
