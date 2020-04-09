package com.appland.appmap.process;

/**
 * Throwing this exception within an invocation hook will skip further execution of the hooked
 * method, instead causing it to return the value passed to the exception.
 */
public class ExitEarly extends RuntimeException {
  private Object returnValue;

  public ExitEarly() {
    super();
  }

  public ExitEarly(Object returnValue) {
    super();
    this.returnValue = returnValue;
  }

  public Object getReturnValue() {
    return this.returnValue;
  }
}
