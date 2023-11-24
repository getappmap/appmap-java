package com.appland.appmap.util;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClassPoolExtension
    implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    AppMapClassPool.acquire(Thread.currentThread().getContextClassLoader());
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    AppMapClassPool.release();
  }
}