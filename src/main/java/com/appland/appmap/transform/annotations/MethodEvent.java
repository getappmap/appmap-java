package com.appland.appmap.transform.annotations;

public enum MethodEvent {
  METHOD_INVOCATION("call", 0),
  METHOD_RETURN("return", 1),
  METHOD_EXCEPTION("return", 2);

  private String eventString;
  private Integer index;

  MethodEvent(String eventString, Integer index) {
    this.eventString = eventString;
    this.index = index;
  }

  public String getEventString() {
    return this.eventString;
  }

  public Integer getIndex() {
    return this.index;
  }
}
