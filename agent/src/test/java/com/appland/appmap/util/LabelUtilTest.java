package com.appland.appmap.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.appland.appmap.test.util.ClassBuilder;
import com.appland.appmap.test.util.MethodBuilder;

public class LabelUtilTest {
  @BeforeAll
  public static void beforeAll() {
    AppMapClassPool.acquire(Thread.currentThread().getContextClassLoader());
  }

  @AfterAll
  public static void afterAll() throws Exception {
    AppMapClassPool.release();
  }

  @Test
  public void detectsLabelsAnnotationByName() throws Exception {
    MethodBuilder mb = new ClassBuilder("LabelUtilTest$Labeled").beginMethod();
    mb.setName("getSecret")
        .setBody("return \"x\";")
        .setReturnType("java.lang.String")
        .addAnnotation(LabelUtil.LABELS_CLASS)
        .endMethod();
    assertTrue(LabelUtil.hasLabelAnnotation(mb.getBehavior()));
  }

  @Test
  public void unlabeledMethodReturnsFalse() throws Exception {
    MethodBuilder mb = new ClassBuilder("LabelUtilTest$Unlabeled").beginMethod();
    mb.setName("getSecret")
        .setBody("return \"x\";")
        .setReturnType("java.lang.String")
        .endMethod();
    assertFalse(LabelUtil.hasLabelAnnotation(mb.getBehavior()));
  }
}
