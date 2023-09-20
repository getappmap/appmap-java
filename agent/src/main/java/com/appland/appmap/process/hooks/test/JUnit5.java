package com.appland.appmap.process.hooks.test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.RecordingSupport;
import com.appland.appmap.reflect.DynamicReflectiveType;
import com.appland.appmap.reflect.ReflectiveType;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.util.ClassUtil;

public class JUnit5 {
  public static final TaggedLogger logger = AppMapConfig.getLogger(null);

  static final String JUNIT_NAME = "junit";

  @ArgumentArray
  @HookClass(value = "org.junit.platform.launcher.core.LauncherConfig$Builder")
  public static void build(Event event, Object receiver, Object[] args) {
    // Gradle uses a separate classloader for the launcher. We need to use that
    // classloader for reflection.
    ClassLoader cl = receiver.getClass().getClassLoader();
    logger.trace("receiver: {}, receiver's class loader: {}", receiver.getClass().getName(),
        cl);

    Builder builder = new Builder(receiver);
    builder.addTestExecutionListener(TestExecutionListener.build(cl));
  }

  private static class Builder extends ReflectiveType {
    private static String ADD_TEST_EXECUTION_LISTENERS = "addTestExecutionListeners";

    private Object listenerArrayArg;

    public Builder(Object self) {
      super(self);

      // addMethod fails here if we pass
      // "[Lorg.junit.platform.launcher.TestExecutionListener;" as the type of
      // the argument method. Whatever the current ClassLoader is can't handle
      // an array of Objects, and throws a ClassNotFoundException. Use
      // java.lang.reflect.Array to manually create an array of the appropriate
      // type, then pass its class to addMethod.
      listenerArrayArg = Array.newInstance(
          ClassUtil.safeClassForName(getClassLoader(), "org.junit.platform.launcher.TestExecutionListener"),
          1);
      addMethod(ADD_TEST_EXECUTION_LISTENERS, listenerArrayArg.getClass());
    }

    public void addTestExecutionListener(Object listener) {
      Array.set(listenerArrayArg, 0, listener);
      invokeVoidMethod(ADD_TEST_EXECUTION_LISTENERS, listenerArrayArg);
    }
  }

  private static class TestIdentifier extends ReflectiveType {
    static class MethodSource extends ReflectiveType {
      private static String GET_CLASS_NAME = "getClassName";
      private static String GET_METHOD_NAME = "getMethodName";
      private static String GET_METHOD_PARAMETER_TYPES = "getMethodParameterTypes";

      public MethodSource(Object self) {
        super(self);
        addMethods(GET_CLASS_NAME, GET_METHOD_NAME, GET_METHOD_PARAMETER_TYPES);
      }

      public String getClassName() {
        return (String) invokeObjectMethod(GET_CLASS_NAME);
      }

      public String getMethodName() {
        return (String) invokeObjectMethod(GET_METHOD_NAME);
      }

      public String getMethodParameterTypes() {
        return (String) invokeObjectMethod(GET_METHOD_PARAMETER_TYPES);
      }
    }

    private static String GET_SOURCE = "getSource";
    private static String IS_TEST = "isTest";

    public TestIdentifier(Object self) {
      super(self);
      addMethods(GET_SOURCE, IS_TEST);
    }

    public Boolean isTest() {
      return (Boolean) invokeObjectMethod(IS_TEST);
    }

    public MethodSource getSource() {
      Optional<?> ret = (Optional<?>) invokeObjectMethod(GET_SOURCE);
      return ret.isPresent() ? new MethodSource(ret.get()) : null;
    }
  }

  private static class TestExecutionResult extends ReflectiveType {
    private static String GET_STATUS = "getStatus";
    private static String GET_THROWABLE = "getThrowable";
    static Enum<?> SUCCESSFUL;

    public TestExecutionResult(Object self) {
      super(self);
      addMethods(GET_STATUS, GET_THROWABLE);
      SUCCESSFUL = ClassUtil.enumValueOf(getClassLoader(), "SUCCESSFUL",
          "org.junit.platform.engine.TestExecutionResult$Status");
    }

    public Object getStatus() {
      return invokeObjectMethod(GET_STATUS);
    }

    public Throwable getThrowable() {
      Optional<?> ret = (Optional<?>) invokeObjectMethod(GET_THROWABLE);
      return ret.isPresent() ? (Throwable) ret.get() : null;
    }

  }

  private static class TestExecutionListener implements InvocationHandler {
    private TestExecutionListener() {
    }

    public static Object build(ClassLoader cl) {
      return DynamicReflectiveType.build(new TestExecutionListener(), cl,
          "org.junit.platform.launcher.TestExecutionListener");
    }

    class JUnit5Details extends RecordingSupport.TestDetails {
      JUnit5Details(TestIdentifier.MethodSource src) {
        if (src == null) {
          return;
        }

        definedClass = src.getClassName();
        isStatic = false; // TODO: add support for static test methods
        methodId = src.getMethodName();

        ClassUtil.MethodLocation loc = ClassUtil.getMethodLocation(src.getClassName(), src.getMethodName(),
            src.getMethodParameterTypes());
        if (loc == null) {
          return;
        }
        path = loc.file;
        lineNumber = String.valueOf(loc.line);

      }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String methodName = method.getName();
      if (methodName.equals("executionStarted")) {
        TestIdentifier id = new TestIdentifier(args[0]);
        logger.trace("executionStarted, id.isTest(): {}, id: {}", id.isTest(), id);
        if (!id.isTest()) {
          return null;
        }
        TestIdentifier.MethodSource src = id.getSource();
        RecordingSupport.startRecording(new JUnit5Details(src), JUNIT_NAME, TestSupport.TEST_RECORDER_TYPE);
      } else if (methodName.equals("executionFinished")) {
        TestIdentifier id = new TestIdentifier(args[0]);
        if (!id.isTest()) {
          return null;
        }
        TestExecutionResult result = new TestExecutionResult(args[1]);
        logger.trace("executionFinished, result: {}", args[1]);

        TestIdentifier.MethodSource src = id.getSource();
        boolean succeeded = result.getStatus().equals(TestExecutionResult.SUCCESSFUL);
        String failureMessage = null;
        int failureLine = -1;
        Throwable t = result.getThrowable();
        JUnit5Details details = new JUnit5Details(src);
        logger.trace(t, "test failed at");
        if (t != null) {
          failureMessage = t.getMessage();
          if (details.definedClass != null && details.methodId != null) {
            for (StackTraceElement ste : t.getStackTrace()) {
              logger.trace("ste: {}", ste);
              if (ste.getClassName().equals(details.definedClass) && ste.getMethodName().equals(details.methodId)) {
                failureLine = ste.getLineNumber();
                break;
              }
            }
          }
        }

        RecordingSupport.stopRecording(details, succeeded, failureMessage, failureLine);

      } else {
        // Don't need any other notifications, default implementations in
        // TestExecutionListener do nothing.
        logger.trace("unhandled method {}", methodName);
      }

      return null;
    }
  }
}