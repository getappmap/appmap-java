package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.Unique;

import java.nio.file.Path;

// @ArgumentArray
// @ExcludeReceiver
// @HookClass("java.nio.channels.FileChannel")
// @Unique("file_access")
public class FileAccess {

  // public static void open(Event event, Object[] args) {
  //   if (args.length < 1) {
  //     return;
  //   }

  //   Path path = (Path) args[0];
  //   System.err.println("open file " + path.toString());
  // }
}