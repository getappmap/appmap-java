package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class HttpServerRequest {
  public String protocol;

  @JSONField(name = "request_method")
  public String method;

  @JSONField(name = "path_info")
  public String path;

  public HttpServerRequest setProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public HttpServerRequest setMethod(String method) {
    this.method = method;
    return this;
  }

  public HttpServerRequest setPath(String path) {
    this.path = path;
    return this;
  }
}