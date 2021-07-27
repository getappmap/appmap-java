package com.appland.appmap.integration;

import com.appland.appmap.config.Properties;
import com.appland.appmap.record.ActiveSessionException;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.test.util.MyClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class RecorderTest {
  private static final Recorder recorder = Recorder.getInstance();

  @Test
  public void testRecordBlock() {
    final MyClass myClass = new MyClass();
    final String scenario = recorder.record(() -> {
      for (int i = 0; i < 10; i++) {
        myClass.myMethod();
      }
    });
    assertNotNull(scenario);
  }

  @Test
  public void testRecordBlockToFile() throws IOException {
    final MyClass myClass = new MyClass();
    final File output = new File(Paths.get(Properties.OutputDirectory, "Recording_a_block_to_a_file.appmap.json").toString());

    if (output.exists()) {
      output.delete();
    }

    recorder.record("Recording a block to a file", () -> {
      for (int i = 0; i < 10; i++) {
        myClass.myMethod();
      }
    });

    assertTrue(output.exists());
  }

  @Test(timeout = 5000)
  public void testMultiThreadedRecordBlock() throws IOException, InterruptedException {
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
