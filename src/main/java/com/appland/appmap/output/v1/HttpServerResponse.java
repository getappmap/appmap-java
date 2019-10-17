package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class HttpServerResponse {
  public Integer status;

  public HttpServerResponse setStatus(Integer status) {
    this.status = status;
    return this;
  }
}