package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Represents a snapshot of an HTTP client request handled at runtime. Embedded within an
 * {@link Event}.
 * @see Event
 * @see <a href="https://github.com/applandinc/appmap#http-client-request-attributes">GitHub: AppMap - HTTP client request attributes</a>
 */
public class HttpClientRequest {
  @JSONField(name = "request_method")
  public String method;

  @JSONField(name = "url")
  public String url;

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
   * Set the URL of this request.
   * 
   * @param url The URL requested
   * @return {@code this}
   * @see <a href=
   *      "https://github.com/applandinc/appmap#http-client-request-attributes">GitHub:
   *      AppMap - HTTP client request attributes</a>
   */
  public HttpClientRequest setURL(String url) {
    this.url = url;
    return this;
  }
}
