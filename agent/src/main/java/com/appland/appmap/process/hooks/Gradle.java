package com.appland.appmap.process.hooks;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.reflect.ReflectiveType;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookClass;

public class Gradle {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  public static class LoaderSpec extends ReflectiveType {
    private static String ALLOW_PACKAGE = "allowPackage";

    public LoaderSpec(Object self) {
      super(self);
      addMethod(ALLOW_PACKAGE, "java.lang.String");
    }

    public void allowPackage(String pkg) {
      invokeVoidMethod(ALLOW_PACKAGE, pkg);
    }
  }

  // FilteringClassLoader was first added to Gradle in v3. Gradle has been
  // hiding the worker classes in the "worker" package since v1.
  @ArgumentArray
  @HookClass(value = "worker.org.gradle.internal.classloader.FilteringClassLoader$Spec")
  public static void allowPackage(Event event, Object receiver, Object[] args) {
    logger.trace("args: {}", args);
    LoaderSpec spec = new LoaderSpec(receiver);

    // We've added our jar to the set searched by the system class loader. Make
    // sure FilterClassLoader doesn't filter out our classes, so they'll get
    // loaded properly.
    spec.allowPackage("com.appland");
  }

}
