package io.spring.cloud.samples.brewery.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.EmitterProcessor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class EventGateway {

	private static final Logger log = LoggerFactory.getLogger(EventGateway.class);

	private final ObjectProvider<EmitterProcessor<Event>> emitterProcessor;

	public EventGateway(ObjectProvider<EmitterProcessor<Event>> emitterProcessor) {
		this.emitterProcessor = emitterProcessor;
	}

	public void emitEvent(Event event) {
		emitterProcessor.ifAvailable(processor -> {
			// [Thread1] Thread Local -> traceId: 1
			log.info("Emitting event [{}]", event);
			processor.onNext(event); 
				// -> [Thread2]
		});
	}

}