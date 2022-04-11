package com.appland.appmap.output.v1;

import java.util.Map;

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

  @JSONField(name = "normalized_path_info")
  public String normalizedPath;

  @JSONField
  public Map<String, String> headers;

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

  /**
   * Set the normalized path for this request.
   */
  public HttpServerRequest setNormalizedPath(String path) {
    this.normalizedPath = path;
    return this;
  }

  /**
   * Set the headers of this request.
   */
  public HttpServerRequest setHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }
}
