package io.spring.cloud.samples.brewery.reporting;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Trace;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
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
	private final TraceManager traceManager;

	@Autowired
	public EventListener(ReportingRepository reportingRepository, TraceManager traceManager) {
		this.reportingRepository = reportingRepository;
		this.traceManager = traceManager;
	}

	@ServiceActivator(inputChannel = EventSink.INPUT)
	public void handleEvents(Event event, @Headers Map<String, Object> headers) {
		Span span = TraceContextHolder.getCurrentSpan();
		log.info("Received the following message with headers [{}] and body [{}]. " +
						"Current TraceID is [{}]", headers, event,
				span != null ? span.getTraceId() : "");
		Trace trace = traceManager.startSpan("updating_reporting", span);
		reportingRepository.createOrUpdate(event);
		traceManager.close(trace);
	}
}
