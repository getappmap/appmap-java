package com.appland.appmap.transform.annotations;

import java.lang.RuntimeException;

public class HookValidationException extends RuntimeException {
  public HookValidationException(String message) {
    super(message);
  }
}