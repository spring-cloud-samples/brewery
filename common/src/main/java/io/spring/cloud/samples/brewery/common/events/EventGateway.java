package io.spring.cloud.samples.brewery.common.events;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import java.util.concurrent.Future;

@MessagingGateway
public interface EventGateway {

	@Gateway(requestChannel=EventSource.OUTPUT)
	Future<Void> emitEvent(Event event);

}