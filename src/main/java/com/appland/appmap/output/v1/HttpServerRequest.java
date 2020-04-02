package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Represents a snapshot of an HTTP server request handled at runtime. Embedded within an
 * {@link Event}.
 * @see Event
 * @see <a href="https://github.com/applandinc/appmap#http-server-request-attributes">GitHub: AppMap - HTTP server request attributes</a>
 */
public class HttpServerRequest {
  public String protocol;

  @JSONField(name = "request_method")
  public String method;

  @JSONField(name = "path_info")
  public String path;

  /**
   * Set the protocol of this request.
   * @param protocol The request protocol
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-request-attributes">GitHub: AppMap - HTTP server request attributes</a>
   */
  public HttpServerRequest setProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  /**
   * Set the method of this request.
   * @param method The request method
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-request-attributes">GitHub: AppMap - HTTP server request attributes</a>
   */
  public HttpServerRequest setMethod(String method) {
    this.method = method;
    return this;
  }

  /**
   * Set the path of this request.
   * @param path The URI path requested
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-request-attributes">GitHub: AppMap - HTTP server request attributes</a>
   */
  public HttpServerRequest setPath(String path) {
    this.path = path;
    return this;
  }
}