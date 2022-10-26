package io.spring.cloud.samples.brewery.bottling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.TestCommunication;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Version;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

class BottlingWorker {


	private static final Logger log = org.slf4j.LoggerFactory.getLogger(BottlingWorker.class);
	private Map<String, State> PROCESS_STATE = new ConcurrentHashMap<>();
	private final ObservationRegistry observationRegistry;
	private final PresentingClient presentingClient;
	private final RestTemplate restTemplate;
	private final EventGateway eventGateway;

	private final BaggageManager baggageManager;

	@Autowired
	public BottlingWorker(ObservationRegistry observationRegistry,
		PresentingClient presentingClient,
		RestTemplate restTemplate, EventGateway eventGateway, BaggageManager baggageManager) {
		this.observationRegistry = observationRegistry;
		this.presentingClient = presentingClient;
		this.restTemplate = restTemplate;
		this.eventGateway = eventGateway;
		this.baggageManager = baggageManager;
	}

	@Async
	public void bottleBeer(Integer wortAmount, String processId) {
		increaseBottles(wortAmount, processId);
		eventGateway.emitEvent(Event.builder().eventType(EventType.BEER_BOTTLED).processId(processId).build());
		notifyPresentingService(processId);
	}

	private void notifyPresentingService(String processId) {
		Observation.createNotStarted("calling_presenting", observationRegistry)
			.observe(() -> {
				String testCommunicationType = TestCommunication.fromBaggage(baggageManager);
				log.info("Found the following communication type [{}]", testCommunicationType);
				if (testCommunicationType.equals("FEIGN")) {
					callPresentingViaFeign(processId);
				}
				else {
					useRestTemplateToCallPresenting(processId);
				}
			});
	}

	private void increaseBottles(Integer wortAmount, String processId) {
		log.info("Bottling beer...");
		Observation.createNotStarted("waiting_for_beer_bottling", observationRegistry)
			.observe(() -> {
				State stateForProcess = PROCESS_STATE.getOrDefault(processId, new State());
				Integer bottled = stateForProcess.bottled;
				Integer bottles = stateForProcess.bottles;
				int bottlesCount = wortAmount / 10;
				bottled += bottlesCount;
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					// i love this construct
				}
				bottles += bottlesCount;
				bottled -= bottlesCount;
				stateForProcess.setBottled(bottled);
				stateForProcess.setBottles(bottles);
				PROCESS_STATE.put(processId, stateForProcess);
			});
	}

	private void callPresentingViaFeign(String processId) {
		presentingClient.updateBottles(PROCESS_STATE.get(processId).getBottles(), processId, "FEIGN");
	}

	private void useRestTemplateToCallPresenting(String processId) {
		restTemplate.exchange(requestEntity()
			.processId(processId)
			.contentTypeVersion(Version.PRESENTING_V1)
			.serviceName(Collaborators.PRESENTING)
			.url("feed/bottles/" + PROCESS_STATE.get(processId).getBottles())
			.httpMethod(HttpMethod.PUT)
			.build(), String.class);
	}

	private static class State {
		private Integer bottles = 0;
		private Integer bottled = 0;

		public State() {
		}

		public Integer getBottles() {
			return this.bottles;
		}

		public Integer getBottled() {
			return this.bottled;
		}

		public void setBottles(Integer bottles) {
			this.bottles = bottles;
		}

		public void setBottled(Integer bottled) {
			this.bottled = bottled;
		}

		public boolean equals(final Object o) {
			if (o == this) return true;
			if (!(o instanceof State)) return false;
			final State other = (State) o;
			if (!other.canEqual((Object) this)) return false;
			final Object this$bottles = this.getBottles();
			final Object other$bottles = other.getBottles();
			if (this$bottles == null ? other$bottles != null : !this$bottles.equals(other$bottles)) return false;
			final Object this$bottled = this.getBottled();
			final Object other$bottled = other.getBottled();
			if (this$bottled == null ? other$bottled != null : !this$bottled.equals(other$bottled)) return false;
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof State;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $bottles = this.getBottles();
			result = result * PRIME + ($bottles == null ? 43 : $bottles.hashCode());
			final Object $bottled = this.getBottled();
			result = result * PRIME + ($bottled == null ? 43 : $bottled.hashCode());
			return result;
		}

		public String toString() {
			return "BottlingWorker.State(bottles=" + this.getBottles() + ", bottled=" + this.getBottled() + ")";
		}
	}
}
