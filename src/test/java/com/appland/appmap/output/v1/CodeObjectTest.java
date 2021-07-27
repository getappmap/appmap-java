package com.appland.appmap.output.v1;

import com.appland.appmap.test.util.ClassBuilder;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CodeObjectTest {

  /**
   * Validate the objects in a CodeObject tree match the given names and types
   * @param obj The root object
   * @param names The names of the objects, beginning with the root
   * @param types The types of the objects, beginning with the root
   */
  private void validateCodeObjectTree(CodeObject obj, String[] names, String[] types) {
    CodeObject currentObject = obj;
    assertTrue(currentObject != null);

    for (int i = 0; currentObject != null; ++i) {
      assertEquals(currentObject.name, names[i]);
      assertEquals(currentObject.type, types[i]);

      List<CodeObject> children = currentObject.getChildren();
      if (children != null && children.size() > 0) {
        currentObject = children.get(0);
      } else {
        currentObject = null;
      }
    }
  }

  @Test
  public void testGetSourceFilePath() {
    CtClass testClass = new ClassBuilder("testGetSourceFilePath1").ctClass();
    assertEquals(CodeObject.getSourceFilePath(testClass), "testGetSourceFilePath1.java");

    testClass = new ClassBuilder("com.myorg.testGetSourceFilePath2").ctClass();
    assertEquals(CodeObject.getSourceFilePath(testClass), "com/myorg/testGetSourceFilePath2.java");

    // It shouldn't be possible to hit this case in the wild, but make sure it
    // doesn't raise an exception
    testClass = new ClassBuilder("").ctClass();
    assertEquals(CodeObject.getSourceFilePath(testClass), ".java");
  }

  @Test
  public void testCreateTree() {
    CtClass testClass = new ClassBuilder("testCreateTree").ctClass();
    validateCodeObjectTree(
      CodeObject.createTree(testClass),
      new String[] { "testCreateTree" },
      new String[] { "class" }
    );

    testClass = new ClassBuilder("com.myorg.testCreateTree").ctClass();
    validateCodeObjectTree(
      CodeObject.createTree(testClass),
      new String[] { "com", "myorg", "testCreateTree" },
      new String[] { "package", "package", "class" }
    );
  }

  @Test
  public void getSourceFilePathForRegularClass() throws NotFoundException {
      CtClass testCtClass = ClassPool.getDefault().get("com.appland.appmap.ExampleClass");
      assertEquals("com/appland/appmap/ExampleClass.java", CodeObject.getSourceFilePath(testCtClass));
  }

  @Test
  public void getSourceFilePath_for_InnerClass_ResultInBaseClass() throws NotFoundException {
      CtClass testCtClass = ClassPool.getDefault().get("com.appland.appmap.output.v1.testclasses.ExampleInnerClass$StaticFinalInnerClass");
      assertEquals("com/appland/appmap/output/v1/testclasses/ExampleInnerClass.java", CodeObject.getSourceFilePath(testCtClass));
  }

  @Test
  public void getSourceFilePath_for_AnonymousClass_ResultInBaseClass() throws NotFoundException {
      CtClass testCtClass = ClassPool.getDefault().get("com.appland.appmap.output.v1.testclasses.Anonymous$1");
      assertEquals("com/appland/appmap/output/v1/testclasses/Anonymous.java", CodeObject.getSourceFilePath(testCtClass));
  }
}
