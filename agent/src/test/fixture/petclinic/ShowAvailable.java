package org.springframework.samples.petclinic.web;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ShowAvailable is a simple Spring Controller that simply returns the number
 * of bytes available in the request's InputStream.
 */
@RestController
public class ShowAvailable {
  private static class Result {
    private final int available;

    public Result(int available) {
      this.available = available;
    }

    public String getAvailable() {
      return Integer.toString(available);
    }
  }

  @PostMapping("/showavailable")
  public String doPost(@RequestBody String body) throws Exception {
    return new ObjectMapper().writeValueAsString(new Result(body.length()));
  }

}
