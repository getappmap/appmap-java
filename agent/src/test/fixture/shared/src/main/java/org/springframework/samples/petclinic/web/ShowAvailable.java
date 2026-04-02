package org.springframework.samples.petclinic.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * ShowAvailable is a simple Spring Controller that simply returns the number
 * of bytes available in the request's InputStream.
 */
@RestController
public class ShowAvailable {

  @PostMapping("/showavailable")
  public String doPost(@RequestBody String body) {
    return String.format("{\"available\":\"%d\"}", body.length());
  }

}
