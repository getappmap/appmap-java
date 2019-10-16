
package com.appland.appmap.record;

import com.appland.appmap.output.v1.Event;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Vector;

public class EventCallStack {
  private Vector<Event> publishedEvents = new Vector<Event>();
  private ArrayDeque<Event> callStack = new ArrayDeque<Event>();

  public EventCallStack() {

  }

  public void add(Event event) {
    if (event.event.equals("call")) {
      callStack.push(event);
      return;
    }

    Event lastEvent = callStack.peek();
    if (lastEvent == null || !lastEvent.methodId.equals(event.methodId)) {
      // note that a method call with no matching return will not be added to the
      // `publishedEvents` array
      System.err.printf("warning: dropping event %s %s.%s()\n",
          event.event,
          event.definedClass,
          event.methodId);
      return;
    }

    Event parentEvent = callStack.pop();
    event.parentId = parentEvent.id;
    publishedEvents.add(parentEvent);
    publishedEvents.add(event);
  }

  public Event[] toArray() {
    Event[] eventArray = new Event[publishedEvents.size()];
    publishedEvents.copyInto(eventArray);
    return eventArray;
  }
}
