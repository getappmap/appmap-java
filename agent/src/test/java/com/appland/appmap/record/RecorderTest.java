package com.appland.appmap.record;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;

public class RecorderTest {

  @BeforeEach
  public void before() throws Exception {
    AppMapConfig.initialize(FileSystems.getDefault());

    final Recorder.Metadata metadata =
        new Recorder.Metadata("recorder_test", "tests");

    Recorder.INSTANCE.start(metadata);
  }

  @AfterEach
  public void after() throws Exception {
    if (Recorder.INSTANCE.hasActiveSession()) {
      Recorder.INSTANCE.stop();
    }
  }

  private static final int EVENT_COUNT = 3;

  private Event newEvent() {
    final Long threadId = Thread.currentThread().getId();
    return new Event()
          .setEvent("call")
          .setDefinedClass("SomeClass")
          .setMethodId("SomeMethod")
          .setStatic(false)
          .setLineNumber(315)
          .setThreadId(threadId);
  }

  private Recorder recordEvents() {
    final Recorder recorder = Recorder.INSTANCE;
    final Event[] events = new Event[3];

    for (int i = 0; i < events.length; i++) {
      events[i] = newEvent();
      recorder.add(events[i]);
      assertEquals(events[i], recorder.getLastEvent());
    }
    return recorder;
  }

  @Test
  public void testCheckpoint() throws IOException {
    Recorder recorder = recordEvents();

    final Recording recording = recorder.checkpoint();
    Path targetPath = recording.moveTo("snapshot.appmap.json");

    // Assert that it's parseable
    InputStream is = new FileInputStream(targetPath.toString());
    Map<?,?> appmap = JSON.parseObject(is, Map.class);
    assertEquals("[classMap, eventUpdates, events, metadata, version]", Arrays.toString(appmap.keySet().stream().sorted().toArray()));
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testUnwriteableOutputFile() throws IOException {
    Recorder recorder = recordEvents();
    final Recording recording = recorder.stop();
    Exception exception = null;
    try {
      recording.moveTo("/no-such-directory/.");
    } catch (RuntimeException e) {
      exception = e;
    }
    assertNotNull(exception);
    assertTrue(exception.toString().indexOf("java.lang.RuntimeException: ") == 0);
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  // for Files.move when: REPLACE_EXISTING, ATOMIC_MOVE
  public void testWriteFileAcrossFilesystems() throws Exception {
    // /dev/shm exists only on Linux
    String sourceFilename = "/tmp/recordertest_file";
    String targetFilename = "/dev/shm/recordertest_file";
    File sourceFile = new File(sourceFilename);
    File targetFile = new File(targetFilename);
    // if the file exists createNewFile returns false
    sourceFile.createNewFile();

    String actualErr = tapSystemErr(() -> {
    // Copying a file across filesystems should not throw an exception.
    final Recording recording = new Recording("recorderName", sourceFile);
    try {
      recording.moveTo(targetFilename);
      } catch (RuntimeException e) {
        fail("recording.moveTo failed, exception, writing across filesystems threw an exception", e);
    } finally {
      sourceFile.delete();
      targetFile.delete();
    }
    });
    assertFalse(actualErr.contains("Invalid cross-device link"));
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  // for Files.move when: REPLACE_EXISTING
  public void testCantOverwriteTargetFile() throws IOException {
    // /dev/shm exists only on Linux
    String sourceFilename = "/tmp/recordertest_file";
    String targetFilename = "/dev/shm/recordertest_file";
    File sourceFile = new File(sourceFilename);
    File targetFile = new File(targetFilename);
    // if the file exists createNewFile returns false
    sourceFile.createNewFile();
    targetFile.createNewFile();

    try {
      // change permissions of destination file so it can't be overwritten
      Set<PosixFilePermission> targetPermissions = PosixFilePermissions.fromString("---------");
      Files.setPosixFilePermissions(targetFile.toPath(), targetPermissions);
    } catch (IOException e) {
    }

    // Copying a file when the destination can't be overwritten should
    // not throw an exception.
    final Recording recording = new Recording("recorderName", sourceFile);
    try {
      recording.moveTo(targetFilename);
    } catch (RuntimeException e) {
      fail("recording.moveTo failed, overwriting the destination threw an exception", e);
    } finally {
      sourceFile.delete();
      targetFile.delete();
    }
  }

  @Test
  public void testAllEventsWritten() throws IOException {
    Recorder recorder = recordEvents();
    final Long threadId = Thread.currentThread().getId();

    final Recording recording = recorder.stop();
    StringWriter sw = new StringWriter();
    recording.readFully(true, sw);
    String appmapJson = sw.toString();
    final String expectedJson = "\"thread_id\":" + threadId;
    final int numMatches = StringUtils.countMatches(appmapJson, expectedJson);
    assertEquals(numMatches, EVENT_COUNT);
  }

  @Test
  public void testMultithreadCheckpoint() throws InterruptedException {
    final Recorder recorder = spy(Recorder.INSTANCE);

    // This puts an entry in recorder.threadState, so there will be a
    // value to iterate over.
    recorder.getLastEvent();


    // Coordinate two threads to ensure that one modifies the
    // Recorder.threadState collection after the other has obtained an
    // iterator on it.
    final Semaphore eventAddedLock = new Semaphore(1);
    final Semaphore iterLock = new Semaphore(1);
    eventAddedLock.acquire();
    iterLock.acquire();

    final ExecutorService es = Executors.newFixedThreadPool(2);
    final Future<?> f1 = es.submit(() -> {
        try {
          iterLock.acquire();
          recorder.add(newEvent());
          eventAddedLock.release();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
    });

    final Future<?> f2 = es.submit(() -> {
        doAnswer((invocation) -> {
            final Object ret = (Iterator<?>)invocation.callRealMethod();
            iterLock.release();
            eventAddedLock.acquire();
            return ret;
      }).when(recorder).getThreadStateIterator();

        recorder.checkpoint();
      });

    try {
      f1.get();
      f2.get();
    } catch (ExecutionException e) {
      // Won't happen if concurrent access to Recorder.threadState is
      // being managed correctly.
      e.getCause().printStackTrace();
      fail();
    }

    es.shutdown();
  }

}
