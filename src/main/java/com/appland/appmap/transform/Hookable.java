package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Hookable is a base class which is used to decide which code should be instrumented by the AppMap agent. Subclasses
 * of Hookable implement various strategies, such as matching by class name, interface name, or method signature.
 */
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
        pairs.addAll(hookable.getBehaviors(classType));
      }

      if (!hasChildren) {
        for (CtBehavior behavior : classType.getDeclaredMethods()) {
          if (hookable.match(behavior)) {
            pairs.add(new BehaviorProcessorPair(behavior, hookable.getProcessorType()));
          }
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
    return true;
  }

  protected Boolean match(CtBehavior behavior) {
    return true;
  }

  private EventProcessorType getProcessorType() {
    return this.processorType;
  }
}
