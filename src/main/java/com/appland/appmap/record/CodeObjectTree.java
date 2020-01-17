
package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class CodeObjectTree {
  private CodeObject root = new CodeObject();

  public CodeObjectTree() {

  }

  private void add(CodeObject rootObject, ArrayList<CodeObject> newObjects) {
    for (CodeObject newObject : newObjects) {
      this.add(rootObject, newObject);
    }
  }

  private void add(CodeObject rootObject, CodeObject newObject) {
    for (CodeObject child : rootObject.children) {
      if (child.equals(newObject)) {
        this.add(child, newObject.children);
        return;
      }
    }
    rootObject.addChild(newObject);
  }

  public void add(CodeObject newObject) {
    this.add(this.root, newObject);
  }

  public void clear() {
    this.root = new CodeObject();
  }

  public Boolean isEmpty() {
    return this.root == null || this.root.children.size() < 1;
  }

  public CodeObject[] toArray() {
    Integer numTopLevelObjects = root.children.size();
    CodeObject[] codeObjects = new CodeObject[numTopLevelObjects];
    for (int i = 0; i < numTopLevelObjects; ++i) {
      codeObjects[i] = root.children.get(i);
    }
    return codeObjects;
  }
}
