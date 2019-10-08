package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class Value {
  public String kind;
  public String name;
  public String value;

  @JSONField(name = "class")
  public String classType;

  @JSONField(name = "object_id")
  public Integer objectId;

  public Value(Object val) {
    this.classType = val.getClass().getName();
    this.objectId = System.identityHashCode(val);
    this.value = val.toString();
  }

  public Value(Object val, String name) {
    this.classType = val.getClass().getName();
    this.name = name;
    this.objectId = System.identityHashCode(val);
    this.value = val.toString();
    this.kind = "req";
  }


}