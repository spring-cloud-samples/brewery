package io.spring.cloud.samples.brewery.bottling;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.TestCommunication;
import io.spring.cloud.samples.brewery.common.model.Version;
import io.spring.cloud.samples.brewery.common.model.Wort;
import org.slf4j.Logger;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

class BottlerService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BottlerService.class);
    private final BottlingWorker bottlingWorker;
    private final PresentingClient presentingClient;
    private final RestTemplate restTemplate;
    private final ObservationRegistry observationRegistry;
    private final CircuitBreakerFactory factory;

    private final BaggageManager baggageManager;

    BottlerService(BottlingWorker bottlingWorker, PresentingClient presentingClient,
            RestTemplate restTemplate, ObservationRegistry observationRegistry, CircuitBreakerFactory factory, BaggageManager baggageManager) {
        this.bottlingWorker = bottlingWorker;
        this.presentingClient = presentingClient;
        this.restTemplate = restTemplate;
        this.observationRegistry = observationRegistry;
        this.factory = factory;
        this.baggageManager = baggageManager;
    }

    void bottle(Wort wort, String processId) {
        factory.create("bottle").run(() -> {
            Observation.createNotStarted("inside_bottling", observationRegistry).observe(() -> {
                log.info("I'm inside bottling");
                bottleWithCircuitBreaker(wort, processId);
            });
            return null;
        });
    }

    /**
     * [OBSERVABILITY] CircuitBreaker integration
     */
    void bottleWithCircuitBreaker(Wort wort, String processId) {
        Observation.createNotStarted("inside_bottling_circuitbreaker", observationRegistry).observe(() -> {
            notifyPresenting(processId);
            factory.create("circuitbreaker").run(() -> {
                bottlingWorker.bottleBeer(wort.getWort(), processId);
                return null;
            });
        });
    }

    void notifyPresenting(String processId) {
        log.info("I'm inside bottling. Notifying presenting");
        String testCommunicationType = TestCommunication.fromBaggage(baggageManager);
        log.info("Found the following communication type [{}]", testCommunicationType);
        if (testCommunicationType.equals("FEIGN")) {
            callPresentingViaFeign(processId);
        }
        else {
            useRestTemplateToCallPresenting(processId);
        }
    }

    private void callPresentingViaFeign(String processId) {
        log.info("Notifying presenting about beer via Feign. Process id [{}]", processId);
        presentingClient.bottlingFeed(processId, "FEIGN");
    }

    private void useRestTemplateToCallPresenting(String processId) {
        log.info("Notifying presenting about beer. Process id [{}]", processId);
        RequestEntity requestEntity = requestEntity()
                .processId(processId)
                .contentTypeVersion(Version.PRESENTING_V1)
                .serviceName(Collaborators.PRESENTING)
                .url("feed/bottling")
                .httpMethod(HttpMethod.PUT)
                .build();
        URI uri = URI.create("http://presenting/feed/bottling");
        restTemplate.put(uri, requestEntity);
    }
}
