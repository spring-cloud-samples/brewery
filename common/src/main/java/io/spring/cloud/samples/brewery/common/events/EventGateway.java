package io.spring.cloud.samples.brewery.common.events;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
public interface EventGateway {

	@Gateway(requestChannel=EventSource.OUTPUT)
	void emitEvent(Event event);

}