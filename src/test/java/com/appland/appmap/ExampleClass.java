package com.appland.appmap;

public class ExampleClass {
  private Integer someState;
  private static Integer someStaticState = 1;

  public ExampleClass() {
    this.someState = 1;
  }

  public static String buildString(Object ... objs) {
    return String.format("%s", objs.toString());
  }

  public static void methodStaticZeroParam() {
    Integer some = 0;
    String local = "variables";
    buildString(ExampleClass.someStaticState, some, local);
  }

  public static void methodStaticSingleParam(Integer x) {
    String local = "variables";
    buildString(ExampleClass.someStaticState, x, local);
  }

  public void methodZeroParam() {
    Integer some = 0;
    String local = "variables";
    buildString(this.someState, some, local);
  }

  public void methodOneParam(Integer x) {
    String local = "variables";
    buildString(this.someState, x, local);
  }
}
