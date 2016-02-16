package io.spring.cloud.samples.brewery.reporting;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Headers;

import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventSink;
import lombok.extern.slf4j.Slf4j;

@MessageEndpoint
@Slf4j
class EventListener {

	private final ReportingRepository reportingRepository;
	private final Tracer tracer;

	@Autowired
	public EventListener(ReportingRepository reportingRepository, Tracer tracer) {
		this.reportingRepository = reportingRepository;
		this.tracer = tracer;
	}

	@ServiceActivator(inputChannel = EventSink.INPUT)
	public void handleEvents(Event event, @Headers Map<String, Object> headers) {
		log.info("Received the following message with headers [{}] and body [{}]", headers, event);
		Span newSpan = tracer.startTrace("local:inside_reporting");
		reportingRepository.createOrUpdate(event);
		log.info("Saved event to the db", headers, event);
		tracer.close(newSpan);
	}
}
