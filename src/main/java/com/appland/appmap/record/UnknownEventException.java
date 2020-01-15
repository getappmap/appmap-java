package com.appland.appmap.record;

import java.lang.RuntimeException;

public class UnknownEventException extends RuntimeException {
  public UnknownEventException(String message) {
    super(message);
  }
}