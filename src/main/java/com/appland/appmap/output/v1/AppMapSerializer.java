package com.appland.appmap.output.v1;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.output.IAppMapSerializer;
import java.io.File;
import java.util.Arrays;
import java.util.Vector;

public class AppMapSerializer implements IAppMapSerializer {
  Vector<CodeObject> codeObjects = new Vector<CodeObject>();
  Vector<Event> events = new Vector<Event>();
  private Integer eventId = 0;

  public AppMapSerializer() {

  }

  private CodeObject getExistingObject(CodeObject codeObject, Iterable<CodeObject> codeObjects) {
    if (codeObjects == null) {
      return null;
    }

    for (CodeObject obj : codeObjects) {
      if (obj.equals(codeObject)) {
        return obj;
      }

      CodeObject childMatch = getExistingObject(codeObject, obj.children);
      if (childMatch != null) {
        return childMatch;
      }
    }
    return null;
  }

  private CodeObject getExistingObject(CodeObject codeObject, CodeObject[] codeObjects) {
    if (codeObjects == null) {
      return null;
    }

    return getExistingObject(codeObject, Arrays.asList(codeObjects));
  }

  @Override
  public void addEvent(Event e) {
    e.id = eventId++;
    events.add(e);
  }

  @Override
  public void addCodeObject(CodeObject obj) {
    CodeObject existingObject = getExistingObject(obj, codeObjects);
    if (existingObject != null) {
      CodeObject[] children = Arrays.copyOf(existingObject.children,
          existingObject.children.length + obj.children.length);

      System.arraycopy(obj.children,
          0, 
          children, 
          existingObject.children.length,
          obj.children.length);

      existingObject.children = children;
      return;
    }

    codeObjects.add(obj);
  }

  @Override
  public String serialize() {
    AppMap appMap = new AppMap();

    appMap.classMap = new CodeObject[codeObjects.size()];
    appMap.events = new Event[events.size()];

    codeObjects.copyInto(appMap.classMap);
    events.copyInto(appMap.events);

    return JSON.toJSONString(appMap);
  }
}