package com.appland.appmap.output.v1;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.data_structures.CodeObjectTree;
import com.appland.appmap.data_structures.EventCallStack;
import com.appland.appmap.output.IAppMapSerializer;
import java.io.File;
import java.util.Arrays;
import java.util.Vector;
import java.util.ArrayDeque;

public class AppMapSerializer implements IAppMapSerializer {

  private CodeObjectTree codeObjects;
  private EventCallStack events;

  public AppMapSerializer(CodeObjectTree codeObjects, EventCallStack events) {
    this.codeObjects = codeObjects;
    this.events = events;
  }

  @Override
  public String serialize() {
    AppMap appMap = new AppMap();
    appMap.classMap = codeObjects.toArray();
    appMap.events = events.toArray();

    return JSON.toJSONString(appMap);
  }
}