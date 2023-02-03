package com.appland.appmap.record;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.Rule;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.output.v1.Event;

public class RecorderTest {

  @Rule
  public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

  @Before
  public void before() throws Exception {
    final Recorder.Metadata metadata =
        new Recorder.Metadata("recorder_test", "tests");

    Recorder.getInstance().start(metadata);
  }

  @After
  public void after() throws Exception {
    if ( Recorder.getInstance().hasActiveSession()) {
      Recorder.getInstance().stop();
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
    final Recorder recorder = Recorder.getInstance();
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
  public void testUnwriteableOutputFile() throws IOException {
    Recorder recorder = recordEvents();
    final Recording recording = recorder.stop();
    Exception exception = null;
    try {
      recording.moveTo("/no-such-directory");
    } catch (RuntimeException e) {
      exception = e;
    }
    assertNotNull(exception);
    assertTrue(exception.toString().indexOf("java.lang.RuntimeException: ") == 0);
  }

  @Test
  // for Files.move when: REPLACE_EXISTING, ATOMIC_MOVE
  public void testWriteFileAcrossFilesystems() throws IOException {
    //  /dev/shm exists only on Linux
    assumeThat(System.getProperty("os.name"), is("Linux"));
    systemErrRule.clearLog();
    String sourceFilename = "/tmp/recordertest_file";
    String targetFilename = "/dev/shm/recordertest_file";
    File sourceFile = new File(sourceFilename);
    File targetFile = new File(targetFilename);
    Exception exception = null;
    // if the file exists createNewFile returns false
    sourceFile.createNewFile();

    // Copying a file across filesystems should not throw an exception.
    final Recording recording = new Recording("recorderName", sourceFile);
    try {
      recording.moveTo(targetFilename);
    } catch (RuntimeException e) {
      exception = e;
      System.out.println(exception.getMessage());
    } finally {
      sourceFile.delete();
      targetFile.delete();
    }
    assertNull("recording.moveTo failed, writing across filesystems threw an exception", exception);
    assertFalse(systemErrRule.getLog().contains("Invalid cross-device link"));
  }

  @Test
  // for Files.move when: REPLACE_EXISTING
  public void testCantOverwriteTargetFile() throws IOException {
    //  /dev/shm exists only on Linux
    assumeThat(System.getProperty("os.name"), is("Linux"));
    String sourceFilename = "/tmp/recordertest_file";
    String targetFilename = "/dev/shm/recordertest_file";
    File sourceFile = new File(sourceFilename);
    File targetFile = new File(targetFilename);
    Exception exception = null;
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
      exception = e;
      System.out.println(exception.getMessage());
    } finally {
      sourceFile.delete();
      targetFile.delete();
    }
    assertNull("recording.moveTo failed, overwriting the destination threw an exception", exception);
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
    final Recorder recorder = spy(Recorder.getInstance());

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
