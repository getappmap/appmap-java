package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import javassist.CtClass;
import javassist.CtBehavior;

public class CodeObject {
  public String name = "";
  public String type = "";
  public String location = "";
  public ArrayList<CodeObject> children = new ArrayList<CodeObject>();

  @JSONField(name = "static")
  public Boolean isStatic;

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj == this) {
      return true;
    }

    if ((obj instanceof CodeObject) == false) {
      return false;
    }

    CodeObject codeObject = (CodeObject)obj;
    return codeObject.type == this.type
        && codeObject.name.equals(this.name)
        && codeObject.isStatic == this.isStatic
        && codeObject.location.equals(this.location);
  }

  public CodeObject() {

  }

  public CodeObject(Package pkg) {
    this.setType("package")
        .setName(pkg.getName());
  }

  public CodeObject(CtClass classType) {
    this.setType("class")
        .setLocation(CodeObject.getSourceFilePath(classType))
        .setName(classType.getSimpleName())
        .setStatic((classType.getModifiers() & Modifier.STATIC) != 0);
  }

  public CodeObject(CtBehavior behavior) {
    String location = String.format("%s:%d",
        CodeObject.getSourceFilePath(behavior.getDeclaringClass()),
        behavior.getMethodInfo().getLineNumber(0));

    this.setType("function")
        .setName(behavior.getName())
        .setLocation(location)
        .setStatic((behavior.getModifiers() & Modifier.STATIC) != 0);
  }

  public CodeObject(CodeObject src) {
    this.setType(src.type)
        .setName(src.name)
        .setStatic(src.isStatic)
        .setLocation(src.location);
  }

  public static String getSourceFilePath(CtClass classType) {
    return String.format("src/main/java/%s/%s",
        classType.getPackageName().replace('.', '/'),
        classType.getClassFile().getSourceFile());
  }

  public static CodeObject createTree(String packageName) {
    String[] packageTokens = packageName.split("\\.");
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

  public static CodeObject createTree(CtClass classType) {
    String packageName = classType.getPackageName();
    CodeObject rootObject = CodeObject.createTree(packageName);
    CodeObject pkgLeafObject = rootObject.get(packageName);
    if (pkgLeafObject == null) {
      System.err.println(String.format("failed to get leaf pkg object for package %s", packageName));
      return null;
    }

    CodeObject classObj = new CodeObject(classType);
    pkgLeafObject.addChild(classObj);

    return rootObject;
  }

  public static CodeObject createTree(CtBehavior method) {
    CtClass classType = method.getDeclaringClass();
    CodeObject rootObject = CodeObject.createTree(classType);
    CodeObject classObject = rootObject.get(classType.getName());

    if (classObject == null) {
      System.err.println(String.format("failed to get class object for package %s", classType.getPackageName()));
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

  public CodeObject findChild(String name, Boolean isStatic, Integer lineNumber) {
    for (CodeObject child : this.children) {
      if (child.name.equals(name)
          && child.isStatic == isStatic 
          && child.location.endsWith(":" + lineNumber.toString())) {
        return child;
      }
    }

    return null;
  }

  public CodeObject findChild(String name) {
    for (CodeObject child : this.children) {
      if (child.name.equals(name)) {
        return child;
      }
    }

    return null;
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