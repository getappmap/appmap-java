package com.appland.appmap.process.hooks;

import static com.appland.appmap.util.ClassUtil.safeClassForName;

import java.util.EnumSet;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.hooks.http.ServletContext;
import com.appland.appmap.process.hooks.http.ServletListener;
import com.appland.appmap.process.hooks.remoterecording.RemoteRecordingFilter;
import com.appland.appmap.reflect.ReflectiveType;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.HookClass;
public class SpringBoot {
  private static final String LISTENER_BEAN = "appmap.listener";
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  static class ApplicationContext extends ReflectiveType {
    private static String GET_BEAN_FACTORY = "getBeanFactory";

    ApplicationContext(Object self) {
      super(self);
      addMethods(GET_BEAN_FACTORY);
    }

    public ConfigurableListableBeanFactory getBeanFactory() {
      return new ConfigurableListableBeanFactory(invokeObjectMethod(GET_BEAN_FACTORY));
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

  // applyInitializers is part of the documented interface of SpringApplication,
  // and has been available since at least v2.7. Should be ok to depend on it.
  @ArgumentArray
  @HookClass(value = "org.springframework.boot.SpringApplication")
  public static void applyInitializers(Event event, Object receiver, Object[] args) {
    if (!Properties.RecordingRequests) {
      logger.debug("request recording disabled");
      return;
    }

    logger.trace("receiver: {}", receiver);

    ApplicationContext ctx = new ApplicationContext(args[0]);
    logger.trace(new Exception(), "ctx: {}", ctx);
    ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
    if (beanFactory.getSingleton(LISTENER_BEAN) == null) {
      Object remoteRecordingFilter = RemoteRecordingFilter.build();
      Object servletListener = ServletListener.build();

      if (remoteRecordingFilter != null && servletListener != null) {
        beanFactory.registerSingleton(LISTENER_BEAN + ".remoteRecordingFilter", remoteRecordingFilter);
        beanFactory.registerSingleton(LISTENER_BEAN, servletListener);
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
  @ExcludeReceiver
  @HookClass(value = "org.springframework.web.SpringServletContainerInitializer")
  public static void onStartup(Event event, Object[] args) {
    ServletContext ctx = new ServletContext(args[1]);
    ctx.addListener(ServletListener.build());
      ServletContext.FilterRegistration fr = ctx.addFilter("com.appland.appmap.RemoteRecordingFilter",
          RemoteRecordingFilter.build());
      fr.addMappingForUrlPatterns(requestEnumSet(), true, "/_appmap/record");
    }

    @SuppressWarnings("unchecked")
    private static EnumSet<?> requestEnumSet() {
      Class<?> dispatcherType;

      if ((dispatcherType = safeClassForName("javax.servlet.DispatcherType")) == null
          && (dispatcherType = safeClassForName("jakarta.servlet.DispatcherType")) == null) {
        throw new InternalError("no DispatcherType class");
      }

      return requestEnumSet(dispatcherType.asSubclass(Enum.class));
    }

    private static <E extends Enum<E>> EnumSet<E> requestEnumSet(Class<E> enumClass) {
      try {
        E requestValue = Enum.valueOf(enumClass, "REQUEST");
        return EnumSet.of(requestValue);
      } catch (IllegalArgumentException e) {
        throw new InternalError("failed to fetch DispatcherType.REQUEST", e);
      }
  }

}
