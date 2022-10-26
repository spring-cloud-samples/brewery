package io.spring.cloud.samples.brewery.reporting;

import java.util.Map;
import java.util.function.Consumer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.TestCommunication;
import io.spring.cloud.samples.brewery.common.events.Event;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component("events")
class EventListener implements Consumer<Message<Event>> {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(EventListener.class);
	private final ReportingRepository reportingRepository;
	private final ObservationRegistry observationRegistry;

	private final BaggageManager baggageManager;

	@Autowired
	public EventListener(ReportingRepository reportingRepository, ObservationRegistry observationRegistry, BaggageManager baggageManager) {
		this.reportingRepository = reportingRepository;
		this.observationRegistry = observationRegistry;
		this.baggageManager = baggageManager;
	}

	private void handleEvents(Event event, Map<String, Object> headers) {
		log.info("Received the following message with headers [{}] and body [{}]", headers, event);
		String testCommunicationType = TestCommunication.fromBaggage(baggageManager);
		log.info("Found the following communication type [{}]", testCommunicationType);
		Observation observation = Observation.createNotStarted("metric.inside.reporting", this.observationRegistry)
			.contextualName("inside_reporting").start();
		observation.observe(() -> {
			reportingRepository.createOrUpdate(event);
			observation.event(new Observation.Event() {
				@Override
				public String getName() {
					return "metric.inside.reporting.savedEvent";
				}

				@Override
				public String getContextualName() {
					return "savedEvent";
				}
			});
			log.info("Saved event to the db [{}] [{}]", headers, event);
		});
	}

	@Override
	public void accept(Message<Event> eventMessage) {
		handleEvents(eventMessage.getPayload(), eventMessage.getHeaders());
	}
}
