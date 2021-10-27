package io.spring.cloud.samples.brewery.presenting.present;

import io.spring.cloud.samples.brewery.presenting.config.Collaborators;
import io.spring.cloud.samples.brewery.presenting.config.Versions;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = Collaborators.BREWING, path = "/ingredients")
public interface BrewingServiceClient {
	@RequestMapping(method = RequestMethod.POST, consumes = Versions.BREWING_CONTENT_TYPE_V1,
			produces = MediaType.APPLICATION_JSON_VALUE)
	String getIngredients(String body,
			@RequestHeader("PROCESS-ID") String processId,
			@RequestHeader("TEST-COMMUNICATION-TYPE") String testCommunicationType);
}
