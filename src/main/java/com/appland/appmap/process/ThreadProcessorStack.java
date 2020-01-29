package com.appland.appmap.process;

import java.util.HashMap;
import java.util.Stack;

public class ThreadProcessorStack {
  private static final HashMap<Long, ThreadProcessorStack> instances =
      new HashMap<Long, ThreadProcessorStack>();

  private final Stack<IEventProcessor> processorStack = new Stack<IEventProcessor>();
  private Boolean isLocked = false;

  private ThreadProcessorStack() { }

  public static ThreadProcessorStack current() {
    Long threadId = Thread.currentThread().getId();
    ThreadProcessorStack instance = ThreadProcessorStack.instances.get(threadId);
    if (instance == null) {
      instance = new ThreadProcessorStack();
      ThreadProcessorStack.instances.put(threadId, instance);
    }
    return instance;
  }

  public Boolean push(IEventProcessor processor) {
    this.processorStack.push(processor);
    return true;
  }

  public IEventProcessor pop() {
    if (this.processorStack.isEmpty()) {
      return null;
    }

    return this.processorStack.pop();
  }

  public Boolean isLocked() {
    return this.isLocked;
  }

  public void setLock(Boolean val) {
    this.isLocked = val;
  }
}
