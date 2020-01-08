package com.appland.appmap.transform.metadata;

import com.appland.appmap.process.EventProcessorType;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class Hookable {
  private ArrayList<Hookable> children = new ArrayList<Hookable>();
  private EventProcessorType processorType = EventProcessorType.Null;

  public Hookable(Hookable ... _children) {
    for (Hookable hookable : _children) {
      this.children.add(hookable);
    }
  }

  public List<BehaviorProcessorPair> getBehaviors(CtClass classType) {
    ArrayList<BehaviorProcessorPair> pairs = new ArrayList<BehaviorProcessorPair>();

    for (Hookable hookable : this.children) {
      Boolean hasChildren = (hookable.children.size() > 0);
      if (hasChildren && hookable.match(classType)) {
        List<BehaviorProcessorPair> childBehaviors = hookable.getBehaviors(classType);
        pairs.addAll(childBehaviors);
      }

      for (CtBehavior behavior : classType.getDeclaredMethods()) {
        if (hookable.match(behavior)) {
          pairs.add(new BehaviorProcessorPair(behavior, hookable.getProcessorType()));
        }
      }
    }

    return pairs;
  }

  public Hookable processedBy(EventProcessorType processorType) {
    this.processorType = processorType;
    return this;
  }

  protected Boolean match(CtClass classType) {
    return false;
  }

  protected Boolean match(CtBehavior behavior) {
    return false;
  }

  private EventProcessorType getProcessorType() {
    return this.processorType;
  }
}