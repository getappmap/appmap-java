package com.appland.appmap.record;

import java.lang.RuntimeException;

public class ActiveSessionException extends RuntimeException {
  public ActiveSessionException(String message) {
    super(message);
  }
}