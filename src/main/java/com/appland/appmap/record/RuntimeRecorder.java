package com.appland.appmap.record;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.output.v1.AppMap;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import java.util.Vector;

public class RuntimeRecorder {
  private static RuntimeRecorder instance = new RuntimeRecorder();

  private CodeObjectTree classMap = new CodeObjectTree();
  private Vector<Event> events = new Vector<Event>();
  private Object mutex = new Object();

  private RuntimeRecorder() { }

  public static RuntimeRecorder get() {
    return RuntimeRecorder.instance;
  }

  public void recordEvent(Event event) {
    synchronized (mutex) {
      events.add(event.freeze());
    }
  }

  public void recordCodeObject(CodeObject codeObject) {
    synchronized (mutex) {
      classMap.add(codeObject);
    }
  }

  public String dumpJson() {
    AppMap appMap = new AppMap();

    synchronized (mutex) {
      appMap.classMap = classMap.toArray();
      appMap.events = new Event[this.events.size()];
      this.events.copyInto(appMap.events);
      this.events.clear();
    }

    return JSON.toJSONString(appMap);
  }
}
