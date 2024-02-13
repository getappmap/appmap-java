package com.example;

import java.io.IOException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;

@Service
public class HttpClientService {

  private static final int CONNECTION_TIMEOUT_MS = 2000; // e.g., 2 seconds

  public boolean get(String url) {

    RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT_MS).build();

    try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {

      HttpGet request = new HttpGet(url);
      try (CloseableHttpResponse response = httpClient.execute(request)) {
        return response.getStatusLine().getStatusCode() == 200;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
}
