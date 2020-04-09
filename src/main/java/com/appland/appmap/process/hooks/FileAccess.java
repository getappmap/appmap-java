package com.appland.appmap.process.hooks;

/**
 * Hooks to capture "file_access" data on event calls.
 */
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
