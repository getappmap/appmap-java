package com.appland.appmap.record;

import java.lang.RuntimeException;

/**
 * This exception is used when a conflict or error occurs in regard to an active recording session.
 */
public class ActiveSessionException extends RuntimeException {
  public ActiveSessionException(String message) {
    super(message);
  }
}
