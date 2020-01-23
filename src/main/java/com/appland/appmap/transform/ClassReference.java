package com.appland.appmap.transform;

/**
* This static class builds a package string and prevents it from being relocated.
*/
class ClassReference {
  public static String create(String... className) {
    return String.join(".", className);
  }
}