package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;
import javassist.CtBehavior;
import javassist.CtClass;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a package, class or method.
 * @see <a href="https://github.com/applandinc/appmap#classmap">GitHub: AppMap - classMap</a>
 */
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

  /**
   * Blank constructor. No initialization.
   */
  public CodeObject() {

  }

  /**
   * Construct a CodeObject representing a Package, setting the type to "package" and copying its
   * name.
   * @param pkg The package representing this CodeObject
   */
  public CodeObject(Package pkg) {
    this.setType("package")
        .setName(pkg.getName());
  }

  /**
   * Construct a CodeObject representing a Class, setting the type to "class" and copying its
   * location, name and flags.
   * @param classType The class representing this CodeObject
   */
  public CodeObject(CtClass classType) {
    this.setType("class")
        .setLocation(CodeObject.getSourceFilePath(classType))
        .setName(classType.getSimpleName())
        .setStatic((classType.getModifiers() & Modifier.STATIC) != 0);
  }

  /**
   * Construct a CodeObject representing a function, setting the type to "function" and copying its
   * name, location and flags.
   * @param behavior The behavior representing this CodeObject
   */
  public CodeObject(CtBehavior behavior) {
    String location = String.format("%s:%d",
        CodeObject.getSourceFilePath(behavior.getDeclaringClass()),
        behavior.getMethodInfo().getLineNumber(0));

    this.setType("function")
        .setName(behavior.getName())
        .setLocation(location)
        .setStatic((behavior.getModifiers() & Modifier.STATIC) != 0);
  }

  /**
   * Copy constructor. Copies type, name, flags and location.
   * @param src The CodeObject to copy from.
   */
  public CodeObject(CodeObject src) {
    this.setType(src.type)
        .setName(src.name)
        .setStatic(src.isStatic)
        .setLocation(src.location);
  }

  /**
   * Guesses the source file path for the given class.
   * @param classType A declared class
   * @return An estimated source file path
   */
  public static String getSourceFilePath(CtClass classType) {
    return String.format("src/main/java/%s/%s",
        classType.getPackageName().replace('.', '/'),
        classType.getClassFile().getSourceFile());
  }

  /**
   * Splits a package name and creates a tree of CodeObjects. For example, "com.appland.demo" would
   * be split into three different "package" CodeObjects: "com", "appland" and "demo".
   * @param packageName A fully qualified package name
   * @return The root CodeObject
   */
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

  /**
   * Create a tree of CodeObjects from the given CtClass. For example, given a CtClass
   * "com.appland.demo.MyClass", a heirarchy of four CodeObjects would be created: "com", "appland",
   * "demo", "MyClass".
   * @param classType The class to create a hierarchy from
   * @return The root of the CodeObject tree
   */
  public static CodeObject createTree(CtClass classType) {
    String packageName = classType.getPackageName();
    CodeObject rootObject = CodeObject.createTree(packageName);
    CodeObject pkgLeafObject = rootObject.get(packageName);
    if (pkgLeafObject == null) {
      System.err.println("failed to get leaf pkg object for package " + packageName);
      return null;
    }

    CodeObject classObj = new CodeObject(classType);
    pkgLeafObject.addChild(classObj);

    return rootObject;
  }

  /**
   * Create a tree of CodeObjects from the given CtBehavior. For example, given a method
   * "com.appland.demo.MyClass.myMethod", a heirarchy of five CodeObjects would be created: "com",
   * "appland", "demo", "MyClass", "myMethod".
   * @param method The method to create a hierarchy from
   * @return The root of the CodeObject tree
   */
  public static CodeObject createTree(CtBehavior method) {
    CtClass classType = method.getDeclaringClass();
    CodeObject rootObject = CodeObject.createTree(classType);
    CodeObject classObject = rootObject.get(classType.getName());

    if (classObject == null) {
      System.err.println("failed to get class object for package " + classType.getPackageName());
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

  /**
   * Iterate a CodeObject tree, searching for an Object by fully qualified path. For example,
   * "com.appland.demo.MyClass"
   * @param path The fully qualified path to search
   * @return The matching CodeObject or null if no CodeObject meets the criteria
   */
  public CodeObject get(String path) {
    List<String> tokens = Arrays.asList(path.split("\\."));
    return this.get(new ArrayDeque<String>(tokens));
  }

  /**
   * Finds an immediate child that matches parameters.
   * @param name The name of the child
   * @param isStatic Whether or not the child is static
   * @param lineNumber The line number of the child
   * @return The child CodeObject, if found. Otherwise, {@code null}.
   */
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

  /**
   * Finds an immediate child that matches parameters.
   * @param name The name of the child
   * @return The child CodeObject, if found. Otherwise, {@code null}.
   */
  public CodeObject findChild(String name) {
    for (CodeObject child : this.children) {
      if (child.name.equals(name)) {
        return child;
      }
    }

    return null;
  }

  /**
   * Set the "name" field.
   * @param name The name of this CodeObject
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes">GitHub: AppMap - Common attributes</a>
   */
  public CodeObject setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Set the "type" field.
   * @param type The type of this CodeObject ({@code package}, {@code class} or {@code function})
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#common-attributes">GitHub: AppMap - Common attributes</a>
   */
  public CodeObject setType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Set the "location" field.
   * @param location The location of this CodeObject
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#function-attributes">AppMap - Function attributes</a>
   */
  public CodeObject setLocation(String location) {
    this.location = location;
    return this;
  }

  /**
   * Set the "static" field.
   * @param isStatic {@code true} if this CodeObject is static
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#function-attributes">AppMap - Function attributes</a>
   */
  public CodeObject setStatic(Boolean isStatic) {
    this.isStatic = isStatic;
    return this;
  }

  /**
   * Add an immediate child to this CodeObject.
   * @param child The child to be added
   * @return {@code this}
   */
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
