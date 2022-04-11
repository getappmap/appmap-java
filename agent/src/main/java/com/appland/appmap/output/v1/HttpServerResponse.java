package com.appland.appmap.output.v1;

import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Represents a snapshot of an HTTP server response sent at runtime. Embedded within an
 * {@link Event}.
 * @see Event
 * @see <a href="https://github.com/applandinc/appmap#http-server-response  -attributes">GitHub: AppMap - HTTP server response   attributes</a>
 */
public class HttpServerResponse {
  public Integer status;

  @JSONField
  public Map<String, String> headers;

  /**
   * Record the status of an HTTP response.
   * @param status The response status returned
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-response-attributes">GitHub: AppMap - HTTP server response attributes</a>
   */
  public HttpServerResponse setStatus(final Integer status) {
    this.status = status;
    return this;
  }

  /**
   * Record the headers of an HTTP response.
   * @param headers The headers to set
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-response-attributes">GitHub: AppMap - HTTP server response attributes</a>
   */
  public HttpServerResponse setHeaders(final Map<String, String> headers) {
    this.headers = headers;
    return this;
  }
}
