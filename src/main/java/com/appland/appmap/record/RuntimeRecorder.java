package com.appland.appmap.record;

import com.appland.appmap.data_structures.CodeObjectTree;
import com.appland.appmap.data_structures.EventCallStack;
import com.appland.appmap.output.v1.AppMapSerializer;
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
    return new AppMapSerializer(classMap, events).serialize();
  }
}
