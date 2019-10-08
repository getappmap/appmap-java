package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

public class CodeObject {
  public String name;
  public String type;
  public String location;
  public ArrayList<CodeObject> children = new ArrayList<CodeObject>();

  @JSONField(name = "static")
  public Boolean isStatic;

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if ((obj instanceof CodeObject) == false) {
      return false;
    }

    CodeObject codeObject = (CodeObject)obj;
    return codeObject.type == type && codeObject.name.equals(name);
  }

  public CodeObject() {

  }

  public CodeObject(Package pkg) {
    this.setType("package")
        .setName(pkg.getName());
  }

  public CodeObject(Class classType) {
    this.setType("class")
        .setName(classType.getSimpleName())
        .setStatic((classType.getModifiers() & Modifier.STATIC) != 0);
  }

  public CodeObject(Executable method) {
    this.setType("function")
        .setName(method.getName())
        .setStatic((method.getModifiers() & Modifier.STATIC) != 0);
  }

  public static CodeObject createTree(Package pkg) {
    String[] packageTokens = pkg.getName().split("\\.");
    CodeObject rootObject = null;
    CodeObject previousObject = null;

    for (String token : packageTokens) {
      CodeObject pkgObject = new CodeObject()
        .setType("package")
        .setName(token);

      if (rootObject == null) {
        rootObject = pkgObject;
      }

      if (previousObject != null) {
        previousObject.addChild(pkgObject);
      }

      previousObject = pkgObject;
    }

    return rootObject;
  }

  public static CodeObject createTree(Class classType) {
    Package pkg = classType.getPackage();
    CodeObject rootObject = CodeObject.createTree(pkg);
    CodeObject pkgLeafObject = rootObject.get(pkg.getName());
    if (pkgLeafObject == null) {
      System.err.println(String.format("failed to get leaf pkg object for package %s", pkg.getName()));
      return null;
    }

    CodeObject classObj = new CodeObject(classType);
    pkgLeafObject.addChild(classObj);

    return rootObject;
  }

  public static CodeObject createTree(Method method) {
    Class classType = method.getDeclaringClass();
    Package pkg = classType.getPackage();
    CodeObject rootObject = CodeObject.createTree(classType);
    CodeObject classObject = rootObject.get(classType.getName());

    if (classObject == null) {
      System.err.println(String.format("failed to get class object for package %s", pkg.getName()));
      return null;
    }

    CodeObject methodObject = new CodeObject(method);
    classObject.addChild(methodObject);

    return rootObject;
  }

  private CodeObject get(ArrayDeque<String> tokens) {
    final String currentToken = tokens.peek();
    if (currentToken == null) {
      return null;
    }

    if (currentToken.equals(this.name)) {
      tokens.pop();
      if (tokens.isEmpty()) {
        return this;
      }

      for (CodeObject child : this.children) {
        CodeObject match = child.get(tokens);
        if (match != null) {
          return match;
        }
      }
    }

    return null;
  }

  public CodeObject get(String path) {
    List<String> tokens = Arrays.asList(path.split("\\."));
    return this.get(new ArrayDeque<String>(tokens));
  }

  public CodeObject setName(String name) {
    this.name = name;
    return this;
  }

  public CodeObject setType(String type) {
    this.type = type;
    return this;
  }

  public CodeObject setLocation(String location) {
    this.location = location;
    return this;
  }

  public CodeObject setStatic(Boolean isStatic) {
    this.isStatic = isStatic;
    return this;
  }

  public CodeObject addChild(CodeObject child) {
    if (child == null) {
      return this;
    }

    if (this.children == null) {
      this.children = new ArrayList<CodeObject>();
    }

    this.children.add(child);

    return this;
  }
}