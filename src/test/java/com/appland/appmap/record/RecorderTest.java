package com.appland.appmap.record;

import com.appland.appmap.output.v1.Event;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RecorderTest {

    @Before
    public void before() throws Exception {
        final IRecordingSession.Metadata metadata =
                new IRecordingSession.Metadata();
        if (!Recorder.getInstance().hasActiveSession())
            Recorder.getInstance().start(metadata);
    }

  @Test
  public void testAllEventsWritten() {
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
                    .setDefinedClass("org.springframework.mock.web.MockFilterChain$ServletFilterProxy")
                    .setEvent("call")
                    .setHttpClientRequest("GET", "/owners/edit", "HTTP/1.1")
                    .setLineNumber(167)
                    .setMethodId("doFilter")
                    .setPath("src/main/java/org/springframework/mock/web/MockFilterChain.java")
                    .setStatic(false)
                    .setThreadId(threadId);

            recorder.add(event);
            assertEquals(event, recorder.getLastEvent());
        }

        final String appmapJson = recorder.stop();
        final String expectedJson = "\"thread_id\":" + threadId.toString();
        final int numMatches = StringUtils.countMatches(appmapJson, expectedJson);
        assertEquals(numMatches, events.length);
    }

    @Test
    public void testReturnEventIsLinkedToParentEvent() {
        Recorder recorder = Recorder.getInstance();
        final Long threadId = Thread.currentThread().getId();

        final Event parentEvent = new Event()
                .setDefinedClass("org.springframework.mock.web.MockFilterChain$ServletFilterProxy")
                .setEvent("call")
                .setHttpClientRequest("GET", "/owners/new", "HTTP/1.1")
                .setLineNumber(167)
                .setMethodId("doFilter")
                .setPath("src/main/java/org/springframework/mock/web/MockFilterChain.java")
                .setStatic(false)
                .setThreadId(threadId);

        final Event returnEvent = new Event()
                .setDefinedClass("org.springframework.mock.web.MockFilterChain$ServletFilterProxy")
                .setEvent("return")
                .setHttpClientResponse(200, "text/html;charset=UTF-8")
                .setLineNumber(167)
                .setMethodId("doFilter")
                .setPath("src/main/java/org/springframework/mock/web/MockFilterChain.java")
                .setStatic(false)
                .setThreadId(threadId);

        assertNull(returnEvent.parentId);

        recorder.add(parentEvent);
        recorder.add(returnEvent);

        assertRecordedReturnEventProperties(recorder.getLastEvent());
        assertNotNull(recorder.getLastEvent().parentId);
        assertEquals(parentEvent.id, recorder.getLastEvent().parentId);

    }

    private void assertRecordedReturnEventProperties(Event event) {
        assertNull(event.definedClass);
        assertNull(event.httpClientRequest);
        assertNull(event.sqlQuery);
        assertNull(event.lineNumber);
        assertNull(event.path);
        assertNull(event.isStatic);
        assertNull(event.methodId);
    }

    @After
    public void tearDown(){
        if (Recorder.getInstance().hasActiveSession())
            Recorder.getInstance().stop();
    }
}
