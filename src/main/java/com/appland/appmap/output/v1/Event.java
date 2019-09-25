package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class Event {
  public Integer id;
  public String event;
  public String path;

  @JSONField(name = "defined_class")
  public String definedClass;

  @JSONField(name = "method_id")
  public String methodId;

  @JSONField(name = "line_no")
  public Integer lineNumber;

  @JSONField(name = "thread_id")
  public Long threadId;

  @JSONField(name = "static")
  public Boolean isStatic;
}