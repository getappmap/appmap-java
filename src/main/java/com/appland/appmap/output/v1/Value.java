package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;

public class Value {
  public String kind;
  public String name;

  @JSONField(serializeUsing = ToStringSerializer.class)
  public Object value;

  @JSONField(name = "class")
  public String classType;

  @JSONField(name = "object_id")
  public Integer objectId;

  public void set(Object val) {
    if (val != null) {
      this.classType = val.getClass().getName();
      this.objectId = System.identityHashCode(val);
      this.value = val;
    } else {
      this.value = null;
      this.objectId = 0;
    }
  }

  public Value() { }

  public Value(Value master) {
    this.classType = master.classType;
    this.kind = master.kind;
    this.name = master.name;
  }

  public Value(Object val) {
    this.set(val);
  }

  public Value(Object val, String name) {
    this.name = name;
    this.kind = "req";
    this.set(val);
  }

  public Value setClassType(String classType) {
    this.classType = classType;
    return this;
  }

  public Value setName(String name) {
    this.name = name;
    return this;
  }

  public Value setKind(String kind) {
    this.kind = kind;
    return this;
  }

  public <T> T get() {
    return (T) this.value;
  }

  public Value freeze() {
    if (this.value != null) {
      this.value = this.value.toString();
    }
    return this;
  }
}