package io.spring.cloud.samples.brewery.reporting;

import java.util.Map;

import brave.Span;
import brave.Tracer;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventSink;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Headers;

@MessageEndpoint
class EventListener {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(EventListener.class);
	private final ReportingRepository reportingRepository;
	private final Tracer tracer;

	@Autowired
	public EventListener(ReportingRepository reportingRepository, Tracer tracer) {
		this.reportingRepository = reportingRepository;
		this.tracer = tracer;
	}

	@ServiceActivator(inputChannel = EventSink.INPUT)
	public void handleEvents(Event event, @Headers Map<String, Object> headers) throws InterruptedException {
		log.info("Received the following message with headers [{}] and body [{}]", headers, event);
		Span newSpan = tracer.nextSpan().name("inside_reporting").start();
		try (Tracer.SpanInScope ws = tracer.withSpanInScope(newSpan)) {
			reportingRepository.createOrUpdate(event);
			newSpan.annotate("savedEvent");
			log.info("Saved event to the db", headers, event);
		} finally {
			newSpan.finish();
		}
	}
}
