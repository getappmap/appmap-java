package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * A serializable snapshot of a runtime exception.
 *
 * @see Event
 */
public class ExceptionValue {
  public String message;
  public String path;

  @JSONField(name = "lineno")
  public int lineNumber;

  @JSONField(name = "class")
  public String classType;

  @JSONField(name = "object_id")
  public Integer objectId;

  /**
   * Constructs from an existing object. Saves the exception class and message.
   */
  public ExceptionValue(Throwable e) {
    this.set(e);
  }

  /**
   * Stores the type, ID and message of an exception.
   */
  public void set(Throwable e) {
    this.classType = e.getClass().getName();
    this.objectId = System.identityHashCode(e);
    StackTraceElement[] stack = e.getStackTrace();
    if ( stack != null && stack.length > 0 ) {
      this.path = stack[0].getFileName();
      this.lineNumber = stack[0].getLineNumber();
    }
    this.message = e.getMessage();
  }

  /**
   * Removes external Object references, preventing
   * future null pointer exceptions and undefined behavior caused by state changes. This should be
   * called once an ExceptionValue is finalized.
   * @return {@code this}
   */
  public ExceptionValue freeze() {
    // All the fields are assigned in the +set+. Nothing to do here afaik.
    return this;
  }
}
