package com.appland.appmap.test.fixture;

public class MyClass {
  public void myMethod() {
    // do nothing
  }

  public void callNonPublic() {
    myPackageMethod();
    myPrivateMethod();
  }

  String myPackageMethod() {
    return "package method";
  }

  private String myPrivateMethod() {
    return "private method";
  }
}
