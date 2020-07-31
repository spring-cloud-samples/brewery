package io.spring.cloud.samples.brewery.reporting;

import java.util.Map;
import java.util.function.Consumer;

import brave.Span;
import brave.Tracer;
import brave.baggage.BaggageField;
import io.spring.cloud.samples.brewery.common.events.Event;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component("events")
class EventListener implements Consumer<Message<Event>> {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(EventListener.class);
	private final ReportingRepository reportingRepository;
	private final Tracer tracer;

	@Autowired
	public EventListener(ReportingRepository reportingRepository, Tracer tracer) {
		this.reportingRepository = reportingRepository;
		this.tracer = tracer;
	}

	private void handleEvents(Event event, Map<String, Object> headers) {
		log.info("Received the following message with headers [{}] and body [{}]", headers, event);
		String testCommunicationType = BaggageField.getByName("TEST-COMMUNICATION-TYPE").getValue();
		log.info("Found the following communication type [{}]", testCommunicationType);
		Span newSpan = tracer.nextSpan().name("inside_reporting").start();
		try (Tracer.SpanInScope ws = tracer.withSpanInScope(newSpan)) {
			reportingRepository.createOrUpdate(event);
			newSpan.annotate("savedEvent");
			log.info("Saved event to the db [{}] [{}]", headers, event);
		} finally {
			newSpan.finish();
		}
	}

	@Override
	public void accept(Message<Event> eventMessage) {
		handleEvents(eventMessage.getPayload(), eventMessage.getHeaders());
	}
}
