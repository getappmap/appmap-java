package com.appland.appmap.transform.annotations;

public enum MethodEvent {
  METHOD_INVOCATION("call"),
  METHOD_RETURN("return"),
  METHOD_EXCEPTION("exception");

  private String eventString;

  MethodEvent(String eventString) {
    this.eventString = eventString;
  }

  public String getEventString() {
    return this.eventString;
  }
}