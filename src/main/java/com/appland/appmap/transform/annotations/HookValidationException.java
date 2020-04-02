package com.appland.appmap.transform.annotations;

import java.lang.RuntimeException;

/**
 * This exception is thrown when a behavior has failed to meet the requirements of a hook.
 */
public class HookValidationException extends RuntimeException {
  public HookValidationException(String message) {
    super(message);
  }
}