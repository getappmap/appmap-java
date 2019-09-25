package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class CodeObject {
  public String name;
  public String type;
  public String location;
  public CodeObject[] children;

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
}