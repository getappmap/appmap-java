package com.appland.appmap;

public class AppTest {
  public static boolean CallMe(int x) {
    if (x % 2 == 0) {
      return true;
    }

    return false;
  }
  public static void main(String[] args) {
    CallMe(1);
  }
}