package com.appland.appmap.process.hooks;

import static com.appland.appmap.util.StringUtil.canonicalName;
import static com.appland.appmap.util.StringUtil.decapitalize;
import static com.appland.appmap.util.StringUtil.identifierToSentence;

import java.util.Objects;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recorder.Metadata;
import com.appland.appmap.record.Recording;

public class RecordingSupport {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private static final Recorder recorder = Recorder.getInstance();

  public static class TestDetails {
    public String definedClass;
    public boolean isStatic;
    public String methodId;
    public String path;
    public String lineNumber;

    protected TestDetails() {
    }

    public TestDetails(Event event) {
      Objects.requireNonNull(event);
      definedClass = event.definedClass;
      isStatic = event.isStatic;
      methodId = event.methodId;
      path = event.path;
      lineNumber = String.valueOf(event.lineNumber);
    }
  }

  public static void startRecording(Event event, Recorder.Metadata metadata) {
    startRecording(new TestDetails(event), metadata);
  }

  public static void startRecording(TestDetails details, Recorder.Metadata metadata) {
    logger.debug("Recording started for {}", canonicalName(details.definedClass, details.isStatic, details.methodId));
    try {
      final String feature = identifierToSentence(details.methodId);
      final String featureGroup = identifierToSentence(details.definedClass);
      metadata.scenarioName = String.format(
          "%s %s",
          featureGroup,
          decapitalize(feature));
      metadata.recordedClassName = details.definedClass;
      metadata.recordedMethodName = details.methodId;
      metadata.sourceLocation = String.join(":", new String[] { details.path, details.lineNumber });
      recorder.start(metadata);
    } catch (ActiveSessionException e) {
      logger.warn(e);
    }
  }

  public static void stopRecording(Event event) {
    RecordingSupport.stopRecording(new TestDetails(event), true, null, null);
  }

  public static void stopRecording(Event event, boolean succeeded) {
    RecordingSupport.stopRecording(new TestDetails(event), succeeded, null, null);
  }

  public static void stopRecording(TestDetails details, Boolean succeeded, String failureMessage,
      Integer failureLine) {
    if (!recorder.hasActiveSession()) {
      return;
    }

    logger.debug("Recording stopped for {}",
        canonicalName(details.definedClass, details.isStatic, details.methodId));
    String filePath = Recorder.sanitizeFilename(String.join("_", details.definedClass, details.methodId));

    Metadata metadata = recorder.getMetadata();
    if (succeeded != null) {
      metadata.testSucceeded = succeeded;
    }
    if (!succeeded) {
      metadata.failureMessage = failureMessage;
      metadata.failureLine = failureLine;
    }
    Recording recording = recorder.stop();
    recording.moveTo(filePath);
  }
}
