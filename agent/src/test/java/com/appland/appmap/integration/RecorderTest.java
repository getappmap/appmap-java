package com.appland.appmap.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystems;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.test.util.MyClass;



public class RecorderTest {
  private static final Recorder recorder = Recorder.getInstance();

  @BeforeEach
  public void initialize() throws Exception {
    AppMapConfig.initialize(FileSystems.getDefault());
  }

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

  @Test
  @Timeout(5)
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
