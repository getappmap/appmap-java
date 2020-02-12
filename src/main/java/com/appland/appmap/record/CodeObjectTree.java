
package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  public CodeObject getMethodBranch(String definedClass,
                                    String methodId,
                                    Boolean isStatic,
                                    Integer lineNumber) {
    final ArrayList<CodeObject> codeObjects = new ArrayList<CodeObject>();
    final List<String> classTokens = Arrays.asList(definedClass.split("\\."));

    CodeObject currentObject = this.root;
    for (String name : classTokens) {
      CodeObject child = currentObject.findChild(name);
      if (child == null) {
        return null;
      }

      codeObjects.add(child);
      currentObject = child;
    }

    CodeObject methodObject = currentObject.findChild(methodId, isStatic, lineNumber);
    if (methodObject == null) {
      return null;
    }
    codeObjects.add(methodObject);

    CodeObject rootObject = null;
    for (CodeObject codeObject : codeObjects) {
      CodeObject newObject = new CodeObject(codeObject);

      if (rootObject == null) {
        rootObject = newObject;
        currentObject = newObject;
        continue;
      }

      currentObject.addChild(newObject);
      currentObject = newObject;
    }

    return rootObject;
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
