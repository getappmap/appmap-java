package src.test.java.com.appland.appmap.output.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.test.util.ClassBuilder;

import javassist.CtClass;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
    assertEquals(CodeObject.getSourceFilePath(testClass), "src/main/java/testGetSourceFilePath1.java");

    testClass = new ClassBuilder("com.myorg.testGetSourceFilePath2").ctClass();
    assertEquals(CodeObject.getSourceFilePath(testClass), "src/main/java/com/myorg/testGetSourceFilePath2.java");
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
}
