package com.appland.appmap.process;

public class ExitEarly extends Throwable {
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