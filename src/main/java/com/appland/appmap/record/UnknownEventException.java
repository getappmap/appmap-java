package com.appland.appmap.record;

import java.lang.RuntimeException;

/**
 * Thrown by the {@link EventRegistry} when an invalid behavior ordinal is accessed.
 */
public class UnknownEventException extends RuntimeException {
  public UnknownEventException(String message) {
    super(message);
  }
}