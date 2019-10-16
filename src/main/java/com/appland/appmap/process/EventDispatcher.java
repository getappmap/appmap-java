package com.appland.appmap.process;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.EventProcessorType;
import com.appland.appmap.process.HttpTomcatReceiver;
import com.appland.appmap.process.NullReceiver;
import com.appland.appmap.process.SqlJdbcReceiver;
import com.appland.appmap.process.PassThroughReceiver;
import com.appland.appmap.record.EventFactory;
import com.appland.appmap.record.RuntimeRecorder;

import java.util.HashMap;

public class EventDispatcher {
  private static HashMap<EventProcessorType, IEventProcessor> eventProcessors =
      new HashMap<>() {{
        put(EventProcessorType.Null, new NullReceiver());
        put(EventProcessorType.PassThrough, new PassThroughReceiver());
        put(EventProcessorType.Http_Tomcat, new HttpTomcatReceiver());
        put(EventProcessorType.Sql_Jdbc, new SqlJdbcReceiver());
      }};

  private static RuntimeRecorder runtimeRecorder = RuntimeRecorder.get();
  private static EventFactory  eventFactory  = EventFactory.get();

  public static void dispatchEvent(EventProcessorType type, Event event) {
    IEventProcessor eventProcessor = EventDispatcher.eventProcessors.get(type);
    if (eventProcessor == null) {
      return;
    }

    Event processedEvent = eventProcessor.processEvent(event);
    if (processedEvent == null) {
      return;
    }

    EventDispatcher.runtimeRecorder.recordEvent(processedEvent);
  }
}
