package io.spring.cloud.samples.brewery.reporting;

import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.trace.SpanContextHolder;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Headers;

import java.util.Map;

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
		Span span = SpanContextHolder.getCurrentSpan();
		log.info("Received the following message with headers [{}] and body [{}]. " +
						"Current traceid is [{}]", headers, event,
				span != null ? span.getTraceId() : "");
		Span newSpan = tracer.joinTrace("inside_reporting", span);
		reportingRepository.createOrUpdate(event);
		log.info("Saved event to the db. Current traceid is [{}]", headers, event,
				span != null ? span.getTraceId() : "");
		tracer.close(newSpan);
	}
}
