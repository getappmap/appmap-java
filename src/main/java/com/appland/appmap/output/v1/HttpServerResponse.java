package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Represents a snapshot of an HTTP server response sent at runtime. Embedded within an
 * {@link Event}.
 * @see Event
 * @see <a href="https://github.com/applandinc/appmap#http-server-response  -attributes">GitHub: AppMap - HTTP server response   attributes</a>
 */
public class HttpServerResponse {
  public Integer status;

  @JSONField(name = "mime_type")
  public String mimeType;

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
   * Record the MIME type of an HTTP response. This is typically the {@code Content-Type} header.
   * @param mimeType The MIME type to set
   * @return {@code this}
   * @see <a href="https://github.com/applandinc/appmap#http-server-response-attributes">GitHub: AppMap - HTTP server response attributes</a>
   */
  public HttpServerResponse setMimeType(final String mimeType) {
    this.mimeType = mimeType;
    return this;
  }
}