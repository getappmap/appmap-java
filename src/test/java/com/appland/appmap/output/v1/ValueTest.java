package com.appland.appmap.output.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.appland.appmap.ExampleClass;

import java.util.Date;

public class ValueTest {

  @Test
  public void freezeCallsCorrectMethod() {
    final ExampleClass myExampleClass = new ExampleClass();
    Value v = new Value(myExampleClass).freeze();
    assertTrue(((String) v.value).matches("^[\\.|\\w]+@[\\da-fA-F]+$"));

    Date date = new Date();
    v = new Value(date).freeze();
    assertEquals((String) v.value, date.toString());
  }
}
