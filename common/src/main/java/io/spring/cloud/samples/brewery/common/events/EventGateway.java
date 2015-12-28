package io.spring.cloud.samples.brewery.common.events;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;

@MessagingGateway(defaultRequestChannel = "amqpOutboundChannel")
public interface EventGateway {

	@Gateway
	void emitEvent(Message<Event> event);
}