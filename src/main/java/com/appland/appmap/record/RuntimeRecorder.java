package com.appland.appmap.record;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.output.v1.AppMap;
import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

public class RuntimeRecorder {
  private static RuntimeRecorder instance = new RuntimeRecorder();

  private CodeObjectTree classMap = new CodeObjectTree();
  private EventCallStack events   = new EventCallStack();

  private RuntimeRecorder() { }

  public static RuntimeRecorder get() {
    return RuntimeRecorder.instance;
  }

  public void recordEvent(Event event) {
    events.add(event);
  }

  public void recordCodeObject(CodeObject codeObject) {
    classMap.add(codeObject);
  }

  public String serializeJson() {
    AppMap appMap = new AppMap();
    appMap.classMap = classMap.toArray();
    appMap.events = events.toArray();

    return JSON.toJSONString(appMap);
  }
}
