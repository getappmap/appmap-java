package com.appland.appmap;

import static org.junit.Assert.assertEquals;

import com.appland.appmap.output.v1.Event;

import com.appland.appmap.record.IRecordingSession;
import com.appland.appmap.record.Recorder;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class LoadJavaMapAgentMojoTest {

    @Before
    public void before() throws Exception {
        final IRecordingSession.Metadata metadata =
                new IRecordingSession.Metadata();

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
                    .setDefinedClass("SomeClass")
                    .setMethodId("SomeMethod")
                    .setStatic(false)
                    .setLineNumber(315)
                    .setThreadId(threadId);

            recorder.add(event);
            assertEquals(event, recorder.getLastEvent());
        }

        final String appmapJson = recorder.stop();
        final String expectedJson = "\"thread_id\":" + threadId.toString();
        final int numMatches = StringUtils.countMatches(appmapJson, expectedJson);
        assertEquals(numMatches, events.length);
    }
}
