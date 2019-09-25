package com.appland.appmap.output;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import java.io.File;

public interface IAppMapSerializer {
  void addEvent(Event e);

  void addCodeObject(CodeObject obj);

  String serialize();
}