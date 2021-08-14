package com.appland.appmap.record;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.output.v1.Event;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class RecorderTest {

  @Before
  public void before() throws Exception {
    final Recorder.Metadata metadata =
        new Recorder.Metadata();

    Recorder.getInstance().start(metadata);
  }

  @After
  public void after() throws Exception {
    if ( Recorder.getInstance().hasActiveSession()) {
      Recorder.getInstance().stop();
    }
  }

  private static final int EVENT_COUNT = 3;

  private Recorder recordEvents() {
    final Recorder recorder = Recorder.getInstance();
    final Long threadId = Thread.currentThread().getId();
    final Event[] events = new Event[] {
        new Event(),
        new Event(),
        new Event(),
    };

    for (int i = 0; i < events.length; i++) {
      final Event event = events[i];
      event
          .setEvent("call")
          .setDefinedClass("SomeClass")
          .setMethodId("SomeMethod")
          .setStatic(false)
          .setLineNumber(315)
          .setThreadId(threadId);

      recorder.add(event);
      assertEquals(event, recorder.getLastEvent());
    }
    return recorder;
  }

  @Test
  public void testCheckpoint() throws IOException {
    Recorder recorder = recordEvents();

    final Recording recording = recorder.checkpoint();
    Path targetPath = FileSystems.getDefault().getPath("build", "tmp", "snapshot.appmap.json");
    recording.moveTo(targetPath.toString());

    // Assert that it's parseable
    InputStream is = new FileInputStream(targetPath.toString());
    Map appmap = JSON.parseObject(is, Map.class);
    assertEquals("[classMap, metadata, version, events]", appmap.keySet().toString());
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
}
