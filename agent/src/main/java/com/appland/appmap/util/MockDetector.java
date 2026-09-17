package com.appland.appmap.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;

/**
 * Recognizes mock objects, so that recording them doesn't call their methods.
 *
 * <p>
 * Calling a method on a mock is an interaction as far as the mocking framework
 * is concerned, and mocking frameworks keep thread-local state between one call
 * and the next. Mockito, for example, collects argument matchers on a
 * thread-local stack and binds them to the next invocation it sees. A stray
 * {@code toString()} from the agent binds that matcher to the wrong call, so
 * the stubbing the test set up never applies and the test fails with a value
 * the user never configured.
 *
 * <p>
 * Detection goes through the mocking framework's own API, because the class
 * name isn't a reliable signal: Mockito's inline mock maker mocks a class by
 * retransforming it, so a mock of {@code com.example.Foo} is an instance of
 * {@code com.example.Foo}.
 */
public class MockDetector {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  /** Placeholder recorded in place of a mock's value. */
  public static final String MOCK_VALUE = "[mocked]";

  /**
   * Types no mock maker will mock, so an instance of one is never a mock.
   *
   * <p>
   * These are the values AppMap records most often, and skipping the lookup for
   * them keeps the common path as cheap as it was before mock detection
   * existed. The list is deliberately conservative -- a type wrongly listed
   * here would reintroduce the bug this class exists to prevent -- and matches
   * what Mockito's {@code MockMaker.isTypeMockable} rejects. Note that enums
   * are absent on purpose: Mockito mocks those quite happily.
   */
  private static final Set<Class<?>> NEVER_MOCKABLE = new HashSet<Class<?>>(Arrays.asList(
      String.class,
      Class.class,
      Boolean.class,
      Byte.class,
      Character.class,
      Short.class,
      Integer.class,
      Long.class,
      Float.class,
      Double.class));

  /**
   * Mockito's mock-detection API, or null if it can't be reached for a given
   * class. Cached per class: for each distinct recorded type, Mockito resolution
   * is attempted once and the result (including "unavailable") is reused.
   */
  private static final ClassValue<Method[]> MOCKITO = new ClassValue<Method[]>() {
    @Override
    protected Method[] computeValue(Class<?> type) {
      for (ClassLoader loader : candidateLoaders(type)) {
        Method[] api = resolve(loader);
        if (api != null) {
          return api;
        }
      }
      return null;
    }
  };

  /**
   * Class loaders that might be able to see Mockito, most specific first.
   *
   * <p>
   * The object's own loader isn't enough. The inline mock maker mocks a class
   * by retransforming it, so a mock can be an instance of any mockable type --
   * including a bootstrap type like {@code java.util.ArrayList}, whose loader
   * can't see Mockito at all.
   *
   * <p>
   * Only loaders that are fixed for the lifetime of the JVM are considered,
   * because the result is cached per class. Including the thread context loader
   * would make the cached answer depend on whichever thread happened to record
   * the first value of that type.
   */
  private static List<ClassLoader> candidateLoaders(Class<?> type) {
    List<ClassLoader> loaders = new ArrayList<ClassLoader>(3);
    addLoader(loaders, type.getClassLoader());
    addLoader(loaders, MockDetector.class.getClassLoader());
    try {
      addLoader(loaders, ClassLoader.getSystemClassLoader());
    } catch (Throwable t) {
      // Can happen very early in startup, or under a security manager.
      logger.debug(t, "couldn't get the system class loader");
    }
    return loaders;
  }

  /** Adds a loader, skipping the bootstrap loader and duplicates. */
  private static void addLoader(List<ClassLoader> loaders, ClassLoader loader) {
    // The bootstrap loader can't see a mocking framework, so there's no point
    // asking it.
    if (loader != null && !loaders.contains(loader)) {
      loaders.add(loader);
    }
  }

  private static Method[] resolve(ClassLoader loader) {
    try {
      Method mockingDetails = Class.forName("org.mockito.Mockito", false, loader)
          .getMethod("mockingDetails", Object.class);
      Method isMock = Class.forName("org.mockito.MockingDetails", false, loader)
          .getMethod("isMock");
      return new Method[] {mockingDetails, isMock};
    } catch (ClassNotFoundException e) {
      // Mockito isn't visible from this loader. Nothing to do, and not worth
      // logging: it's the normal case outside of tests.
      return null;
    } catch (Throwable t) {
      // A Mockito we don't recognize. Degrade to treating objects as
      // non-mocks rather than failing.
      logger.debug(t, "couldn't find Mockito's mock detection API");
      return null;
    }
  }

  private MockDetector() {
  }

  /**
   * Checks whether an object is a mock. Only inspects the mocking framework's
   * bookkeeping; never calls a method on {@code o} itself.
   *
   * @param o the object to check, may be null
   * @return true if {@code o} is known to be a mock
   */
  public static boolean isMock(Object o) {
    if (o == null) {
      return false;
    }

    Class<?> type = o.getClass();
    if (type.isArray() || NEVER_MOCKABLE.contains(type)) {
      return false;
    }

    Method[] api = MOCKITO.get(type);
    if (api == null) {
      return false;
    }

    try {
      Object details = api[0].invoke(null, o);
      return details != null && Boolean.TRUE.equals(api[1].invoke(details));
    } catch (Throwable t) {
      // If we can't tell, say no: recording a value is less important than not
      // guessing wrong about the user's objects.
      logger.debug(t, "failed to check whether {} is a mock", type.getName());
      return false;
    }
  }
}
