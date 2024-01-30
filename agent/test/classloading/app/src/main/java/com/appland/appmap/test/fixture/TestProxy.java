package com.appland.appmap.test.fixture;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.util.ClassUtil;

public class TestProxy implements TestClass {
  private static final String HW_CLASS_NAME =
      MethodHandles.lookup().lookupClass().getPackage().getName() + ".helloworld.HelloWorld";

  @Override
  public int beforeTest() throws Exception {
    // Sanity check
    try {
      Class.forName(HW_CLASS_NAME);
      System.err.println("Misconfigured, " + HW_CLASS_NAME + " shouldn't be on class path");
      return 1;
    } catch (ClassNotFoundException e) {
      // expected
    }
    return 0;
  }

  private static class HWInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("getGreeting")) {
        return "Hello World!";
      }

      return null;
    }
  }


  @Override
  public int runTest() throws IOException {
    try {
      Recorder recorder = Recorder.getInstance();
      Recording recording = recorder.record(() -> {
        try {
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          Class<?>[] hwClass = {Class.forName(HW_CLASS_NAME, true, cl)};
          InvocationHandler hwHandler = new HWInvocationHandler();
          Object proxy = Proxy.newProxyInstance(cl, hwClass, hwHandler);
          Method getGreeting = proxy.getClass().getMethod("getGreeting", Integer.TYPE);
          getGreeting.invoke(proxy, Integer.valueOf(1));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });

      StringWriter sw = new StringWriter();
      recording.readFully(true, sw);
      System.out.println(sw.toString());

      return 0;
    } catch (

    Exception e) {
      e.printStackTrace();
    }
    return 1;
  }



}
