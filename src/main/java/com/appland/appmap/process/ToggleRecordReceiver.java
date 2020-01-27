package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;


public class ToggleRecordReceiver implements IEventProcessor {
  private static final Recorder recorder = Recorder.getInstance();

  public ToggleRecordReceiver() {
  }

  @Override
  public Boolean processEvent(Event event, ThreadLock lock) {
    try {
      if (event.event.equals("call")) {
        final String fileName = String.join("_", event.definedClass, event.methodId)
                                      .replaceAll("^[a-zA-Z0-9-_]", "_");

        IRecordingSession.Metadata metadata = new IRecordingSession.Metadata();

        // TODO: Obtain this info in the constructor
        boolean junit = false;
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 0; !junit && i < stack.length; i++) {
          if ( stack[i].getClassName().startsWith("org.junit") ) {
            junit = true;
          }
        }

        metadata.recordedClassName = event.definedClass;
        metadata.recordedMethodName = event.methodId;
        if ( junit ) {
          metadata.recorderName = "toggle_record_receiver";
          metadata.framework = "junit";
        }

        recorder.start(fileName, metadata);
      } else {
        recorder.stop();
      }
    } catch (ActiveSessionException e) {
      System.err.printf("AppMap: %s\n", e.getMessage());
    }

    return true;
  }
}
