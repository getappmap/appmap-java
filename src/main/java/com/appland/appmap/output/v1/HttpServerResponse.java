package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class HttpServerResponse {
  public Integer status;

  @JSONField(name = "mime_type")
  public String mimeType;

  public HttpServerResponse setStatus(Integer status) {
    this.status = status;
    return this;
  }

  public HttpServerResponse setMimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }
}