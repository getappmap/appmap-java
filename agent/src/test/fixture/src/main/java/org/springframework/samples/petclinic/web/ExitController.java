package org.springframework.samples.petclinic.web;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ExitController {
	@DeleteMapping("/exit")
	public void exit() {
		System.exit(0);
	}
}
