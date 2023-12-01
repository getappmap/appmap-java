
package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;

import java.util.List;

/**
 * Utility class representing the root of a {@link CodeObject} tree. Contains methods to make tree
 * manipulation easier.
 */
public class CodeObjectTree {
  private CodeObject root = new CodeObject();

  public CodeObjectTree() {

  }

  private void add(CodeObject rootObject, List<CodeObject> newObjects) {
    for (CodeObject newObject : newObjects) {
      this.add(rootObject, newObject);
    }
  }

  private void add(CodeObject rootObject, CodeObject newObject) {
    for (CodeObject child : rootObject.safeGetChildren()) {
      if (child.equals(newObject)) {
        this.add(child, newObject.safeGetChildren());
        return;
      }
    }
    rootObject.addChild(newObject);
  }

  /**
   * Recursively add a {@link CodeObject} hierarchy to this tree.
   * @param newObject The root of the tree to be added
   */
  public void add(CodeObject newObject) {
    this.add(this.root, newObject);
  }

  /**
   * Clear all {@link CodeObject}s from this tree.
   */
  public void clear() {
    this.root = new CodeObject();
  }

  /*
   * Count occurrences of delim in s. Assumes s is well-formed,
   * i.e. doesn't have delim as the first or last character
   */
  private int countTokens(String s, char delim) {
    if (s.length() == 0) {
      return 0;
    }
    
    int start = 0, end = 0, count = 0;
    while ((end = s.indexOf(delim, start)) > 0) {
      start = end + 1;
      count++;
    }
    return ++count;
  }

  /**
   * Finds a single method and returns the entire branch as a tree.
   * @param definedClass The declaring class name
   * @param methodId The method name
   * @param isStatic Is the method static?
   * @param lineNumber The method line number
   * @return The root of the tree, if found. Otherwise, {@code null}.
   */
  public CodeObject getMethodBranch(String definedClass,
                                    String methodId,
                                    Boolean isStatic,
                                    Integer lineNumber) {
    int tokenCount = countTokens(definedClass, '.');
    final CodeObject[] codeObjects = new CodeObject[tokenCount + 1]; // + 1 for the method itself
    int idx = 0;
    
    CodeObject currentObject = this.root;
    int start = 0, end = 0;
    // Avoid allocating a String[] by scanning definedClass again.
    while ((end = definedClass.indexOf('.', start)) > 0) {
      CodeObject child = currentObject.findChildBySubstring(definedClass, start, end);
      if (child == null) {
        return null;
      }
      start = end + 1;
      codeObjects[idx++] = currentObject = child;
    }
    assert definedClass.length() - start > 0 : "Not enough tokens";  // Should be one more token
    CodeObject child = currentObject.findChildBySubstring(definedClass, start, definedClass.length());
    if (child == null) {
      return null;
    }
    codeObjects[idx++] = currentObject = child;
    
    CodeObject methodObject = currentObject.findChild(methodId, isStatic, lineNumber);
    if (methodObject == null) {
      return null;
    }
    codeObjects[idx] = methodObject;

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

  /**
   * Check if this tree is empty
   * @return {@code true} if the root node is null or contains no children. Otherwise,
   *         {@code false}.
   */
  public Boolean isEmpty() {
    return this.root == null || this.root.safeGetChildren().size() < 1;
  }

  /**
   * Flattens the hierarchy and returns all {@link CodeObject}s as an array.
   * @return A flattened array of {@link CodeObject}s
   */
  public CodeObject[] toArray() {
    List<CodeObject> children = root.safeGetChildren();
    Integer numTopLevelObjects = children.size();
    CodeObject[] codeObjects = new CodeObject[numTopLevelObjects];
    for (int i = 0; i < numTopLevelObjects; ++i) {
      codeObjects[i] = children.get(i);
    }
    return codeObjects;
  }
}
