package com.appland.appmap.integration;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.test.util.MyClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class RecorderTest {
  private static final Recorder recorder = Recorder.getInstance();

  @Test
  public void testRecordBlock() {
    final MyClass myClass = new MyClass();
    final Recording recording = recorder.record(() -> {
      for (int i = 0; i < 10; i++) {
        myClass.myMethod();
      }
    });
    assertNotNull(recording);
  }

  @Test
  public void testRecordBlockToFile() throws IOException {
    final MyClass myClass = new MyClass();

    Recording recording = recorder.record(() -> {
      for (int i = 0; i < 10; i++) {
        myClass.myMethod();
      }
    });

    assertNotNull(recording);
    StringWriter sw = new StringWriter();
    recording.readFully(true, sw);
    // Verify that the JSON parses properly.
    JSON.parse(sw.toString());
  }

  @Test(timeout = 5000)
  public void testMultiThreadedRecordBlock() throws InterruptedException {
    final int iterations = 1000;
    final MyClass myClass = new MyClass();
    Thread t = new Thread(() -> {
      for (int i = 0; i < iterations; i++) {
        try {
          recorder.record(() -> {
            myClass.myMethod();
          });
        } catch (ActiveSessionException e) {
          // good, continue
        }
      }
    });

    t.start();
    for (int i = 0; i < iterations; i++) {
      try {
        recorder.record(() -> {
          myClass.myMethod();
        });
      } catch (ActiveSessionException e) {
        // good, continue
      }
    }

    t.join();
    assertFalse(recorder.hasActiveSession());
  }
}
