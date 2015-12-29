package io.spring.cloud.samples.brewery.common.events;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Publisher;
import org.springframework.messaging.Message;

@MessageEndpoint
public class EventGateway {

	@Publisher(channel = EventSource.OUTPUT)
	public Message<Event> emitEvent(Message<Event> event) {
		return event;
	}

}