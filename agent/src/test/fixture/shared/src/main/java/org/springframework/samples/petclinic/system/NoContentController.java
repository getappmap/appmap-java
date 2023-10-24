package org.springframework.samples.petclinic.system;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NoContentTypeController {

	@RequestMapping("/no-content")
	public HttpEntity<?> noContent() {
		return HttpEntity.EMPTY;
	}

}
