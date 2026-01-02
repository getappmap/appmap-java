package com.appland.appmap.process.hooks;

import static com.appland.appmap.util.ClassUtil.safeClassForNames;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.http.ServletContext;
import com.appland.appmap.process.hooks.http.ServletListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.appland.appmap.reflect.DynamicReflectiveType;
import com.appland.appmap.process.hooks.remoterecording.RemoteRecordingFilter;
import com.appland.appmap.process.hooks.remoterecording.RemoteRecordingManager;
import com.appland.appmap.reflect.ReflectiveType;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.util.ClassUtil;

public class SpringBoot {
  private static final String SERVLET_CONTEXT_INITIALIZED = "com.appland.appmap.ServletContextInitialized";
  private static final String LISTENER_BEAN = "appmap.listener";
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  static class ApplicationContext extends ReflectiveType {
    private static String GET_BEAN_FACTORY = "getBeanFactory";
    private static String GET_SERVLET_CONTEXT = "getServletContext";

    ApplicationContext(Object self) {
      super(self);
      addMethods(GET_BEAN_FACTORY, GET_SERVLET_CONTEXT);
    }

    public ConfigurableListableBeanFactory getBeanFactory() {
      return new ConfigurableListableBeanFactory(invokeObjectMethod(GET_BEAN_FACTORY));
    }

    public ServletContext getServletContext() {
      Object ret = invokeObjectMethod(GET_SERVLET_CONTEXT);
      return ret != null ? new ServletContext(ret) : null;
    }
  }

  static class ConfigurableListableBeanFactory extends ReflectiveType {
    private static final String GET_SINGLETON = "getSingleton";
    private static final String REGISTER_SINGLETON = "registerSingleton";

    ConfigurableListableBeanFactory(Object self) {
      super(self);
      addMethod(GET_SINGLETON, String.class);
      addMethod(REGISTER_SINGLETON, String.class, Object.class);
    }

    public Object getSingleton(String name) {
      return invokeObjectMethod(GET_SINGLETON, name);
    }

    public void registerSingleton(String name, Object bean) {
      invokeVoidMethod(REGISTER_SINGLETON, name, bean);
    }
  }

  private static class ApplicationListener implements InvocationHandler {
    private final TaggedLogger logger;

    public ApplicationListener(TaggedLogger logger) {
      this.logger = logger;
    }

    public static Object build(ClassLoader cl, TaggedLogger logger) {
      return DynamicReflectiveType.build(new ApplicationListener(logger), cl, "org.springframework.context.ApplicationListener");
    }

    @SuppressWarnings("SuspiciousInvocationHandlerImplementation") // handled by DynamicReflectiveType
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      if (method.getName().equals("onApplicationEvent")) {
        Object event = args[0];
        // WebServerInitializedEvent is a base class for generic web server events
        if (event.getClass().getSimpleName().contains("WebServerInitializedEvent")) {
          try {
            WebServerInitializedEvent webServerEvent = new WebServerInitializedEvent(event);
            WebServer webServer = new WebServer(webServerEvent.getWebServer());
            int port = webServer.getPort();

            String contextPath = getContextPath(event);

            RemoteRecordingManager.logServerStart(port, contextPath);
          } catch (Exception e) {
            logger.debug(e, "Failed to retrieve server URL in SpringBoot hook");
          }
        }
      }
      return null;
    }

    private static String getContextPath(Object event) {
      String contextPath = null;
      if (event.getClass().getSimpleName().equals("ServletWebServerInitializedEvent")) {
        ServletWebServerInitializedEvent servletEvent = new ServletWebServerInitializedEvent(event);
        ApplicationContext appCtx = servletEvent.getApplicationContext();
        ServletContext servletCtx = appCtx.getServletContext();
        if (servletCtx != null) {
          contextPath = servletCtx.getContextPath();
        }
      }
      return contextPath;
    }
  }

  private static class ServletWebServerInitializedEvent extends WebServerInitializedEvent {
    private static final String GET_APPLICATION_CONTEXT = "getApplicationContext";

    public ServletWebServerInitializedEvent(Object self) {
      super(self);
      addMethods(GET_APPLICATION_CONTEXT);
    }

    public ApplicationContext getApplicationContext() {
      return new ApplicationContext(invokeObjectMethod(GET_APPLICATION_CONTEXT));
    }
  }

  private static class WebServerInitializedEvent extends ReflectiveType {
    private static final String GET_WEB_SERVER = "getWebServer";

    public WebServerInitializedEvent(Object self) {
      super(self);
      addMethods(GET_WEB_SERVER);
    }

    public Object getWebServer() {
      return invokeObjectMethod(GET_WEB_SERVER);
    }
  }

  private static class WebServer extends ReflectiveType {
    private static final String GET_PORT = "getPort";

    public WebServer(Object self) {
      super(self);
      addMethods(GET_PORT);
    }

    public int getPort() {
      return (int) invokeObjectMethod(GET_PORT);
    }
  }

  private static class SpringApplication extends ReflectiveType {
    private static final String ADD_LISTENERS = "addListeners";

    public SpringApplication(Object self) {
      super(self);
      // Parameter type is ApplicationListener... which is an array
      addMethod(ADD_LISTENERS, "[Lorg.springframework.context.ApplicationListener;");
    }

    public void addListeners(Object... listeners) {
      // Create an array of the expected type
      try {
        ClassLoader cl = self.getClass().getClassLoader();
        Class<?> listenerClass = safeClassForNames(cl, "org.springframework.context.ApplicationListener");
        Object listenerArray = java.lang.reflect.Array.newInstance(listenerClass, listeners.length);
        for (int i = 0; i < listeners.length; i++) {
          java.lang.reflect.Array.set(listenerArray, i, listeners[i]);
        }
        invokeVoidMethod(ADD_LISTENERS, listenerArray);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  // applyInitializers is part of the documented interface of SpringApplication,
  // and has been available since at least v2.7. Should be ok to depend on it.
  @ArgumentArray
  @HookClass(value = "org.springframework.boot.SpringApplication", methodEvent = MethodEvent.METHOD_RETURN)
  public static void applyInitializers(Event event, Object receiver, Object ret, Object[] args) {
    ApplicationContext appCtx = new ApplicationContext(args[0]);
    logger.trace("ctx: {}", appCtx);

    try {
      ClassLoader cl = receiver.getClass().getClassLoader();
      SpringApplication springApp = new SpringApplication(receiver);
      Object listenerProxy = ApplicationListener.build(cl, logger);
      springApp.addListeners(listenerProxy);
    } catch (Exception e) {
      logger.error("Failed to add appmap listener to SpringApplication", e);
    }

    ServletContext servletCtx = appCtx.getServletContext();
    if (servletCtx != null) {
      Object initializedAttr = servletCtx.getAttribute(SERVLET_CONTEXT_INITIALIZED);
      if (initializedAttr != null && ((Boolean) initializedAttr).booleanValue()) {
        logger.trace("servlet context initialized");
        return;
      }
    }

    ConfigurableListableBeanFactory beanFactory = appCtx.getBeanFactory();
    if (beanFactory.getSingleton(LISTENER_BEAN) == null) {
      ClassLoader cl = receiver.getClass().getClassLoader();
      Object remoteRecordingFilter = RemoteRecordingFilter.build(cl);
      Object servletListener = ServletListener.build(cl);

      if (remoteRecordingFilter != null && servletListener != null) {
        beanFactory.registerSingleton(LISTENER_BEAN + ".remoteRecordingFilter", remoteRecordingFilter);
        if (Properties.RecordingRequests) {
          logger.trace("registering servlet listener as singleton");
          beanFactory.registerSingleton(LISTENER_BEAN, servletListener);
        }
        logger.trace("registered beans");
      } else {
        logger.trace("a bean is null, remoteRecordingFilter: {} servletListener: {}", remoteRecordingFilter,
            servletListener);
      }

      logger.trace("initialized context");
    } else {
      logger.trace("already initialized");
    }
  }

  @ArgumentArray
  @HookClass(value = "org.springframework.web.SpringServletContainerInitializer")
  public static void onStartup(Event event, Object receiver, Object[] args) {
    ClassLoader cl = receiver.getClass().getClassLoader();
    ServletContext ctx = new ServletContext(args[1]);
    logger.trace("ctx: {}", ctx);

    if (Properties.RecordingRequests) {
      logger.trace("adding listener to sevlet context");
      ctx.addListener(ServletListener.build(cl));
    } else {
      logger.debug("request recording disabled");
    }

    ServletContext.FilterRegistration fr = ctx.addFilter("com.appland.appmap.RemoteRecordingFilter",
        RemoteRecordingFilter.build(cl));
    fr.addMappingForUrlPatterns(
        ClassUtil.enumSetOf(
            safeClassForNames(cl, "javax.servlet.DispatcherType", "jakarta.servlet.DispatcherType")
                .asSubclass(Enum.class),
            "REQUEST"),
        true, RemoteRecordingManager.RecordRoute);

    ctx.setAttribute(SERVLET_CONTEXT_INITIALIZED, Boolean.TRUE);
  }

}
