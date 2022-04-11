package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Represents a snapshot of an HTTP client request handled at runtime. Embedded within an
 * {@link Event}.
 * @see Event
 * @see <a href="https://github.com/applandinc/appmap#http-client-request-attributes">GitHub: AppMap - HTTP client request attributes</a>
 */
public class HttpClientRequest {
  public String protocol;

  @JSONField(name = "request_method")
  public String method;

  @JSONField(name = "path_info")
  public String path;

  @JSONField(name = "normalized_path_info")
  public String normalizedPath;

  /**
   * Set the protocol of this request.
   * @param protocol The request protocol
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-client-request-attributes">GitHub: AppMap - HTTP client request attributes</a>
   */
  public HttpClientRequest setProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  /**
   * Set the method of this request.
   * @param method The request method
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-client-request-attributes">GitHub: AppMap - HTTP client request attributes</a>
   */
  public HttpClientRequest setMethod(String method) {
    this.method = method;
    return this;
  }

  /**
   * Set the path of this request.
   * @param path The URI path requested
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-client-request-attributes">GitHub: AppMap - HTTP client request attributes</a>
   */
  public HttpClientRequest setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * Set the normalized path for this request.
   */
  public HttpClientRequest setNormalizedPath(String path) {
    this.normalizedPath = path;
    return this;
  }
}
