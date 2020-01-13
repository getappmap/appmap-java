package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import javassist.CtBehavior;

public class BehaviorProcessorPair {
  private CtBehavior behavior;
  private EventProcessorType processorType = EventProcessorType.Null;

  public BehaviorProcessorPair(CtBehavior behavior, EventProcessorType processorType) {
    this.behavior = behavior;
    this.processorType = processorType;
  }

  public CtBehavior getBehavior() {
    return this.behavior;
  }

  public EventProcessorType getProcessorType() {
    return this.processorType;
  }
}
