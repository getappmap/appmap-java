package com.appland.appmap.output.v1;

import java.lang.RuntimeException;

/**
 * Thrown when trying to access information from the source code which does not exist. For example,
 * this event could be thrown if a methods parameter identifiers are not be available.
 */
public class NoSourceAvailableException extends RuntimeException {
  public NoSourceAvailableException() {
    super();
  }
}