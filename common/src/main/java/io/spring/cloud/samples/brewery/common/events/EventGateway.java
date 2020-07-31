package io.spring.cloud.samples.brewery.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class EventGateway {

	private static final Logger log = LoggerFactory.getLogger(EventGateway.class);

	private final ObjectProvider<StreamBridge> streamBridge;

	public EventGateway(ObjectProvider<StreamBridge> streamBridge) {
		this.streamBridge = streamBridge;
	}

	public void emitEvent(Event event) {
		streamBridge.ifAvailable(processor -> {
			// [Thread1] Thread Local -> traceId: 1
			log.info("Emitting event [{}]", event);
			processor.send("events-out-0", MessageBuilder.withPayload(event).build());
				// -> [Thread2]
		});
	}

}