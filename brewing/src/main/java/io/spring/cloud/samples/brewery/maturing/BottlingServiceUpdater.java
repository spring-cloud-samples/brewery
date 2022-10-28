package io.spring.cloud.samples.brewery.maturing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.BottlingService;
import io.spring.cloud.samples.brewery.common.TestCommunication;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.model.Version;
import io.spring.cloud.samples.brewery.common.model.Wort;
import org.slf4j.Logger;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

class BottlingServiceUpdater {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BottlingServiceUpdater.class);
    private final BrewProperties brewProperties;
    private final ObservationRegistry observationRegistry;
    private final PresentingServiceClient presentingServiceClient;
    private final BottlingService bottlingService;
    private final RestTemplate restTemplate;
    private final EventGateway eventGateway;
    private final CircuitBreakerFactory circuitBreakerFactory;

    private final BaggageManager baggageManager;

    BottlingServiceUpdater(BrewProperties brewProperties,
            ObservationRegistry observationRegistry,
            PresentingServiceClient presentingServiceClient,
            BottlingService bottlingService,
            RestTemplate restTemplate, EventGateway eventGateway, CircuitBreakerFactory circuitBreakerFactory, BaggageManager baggageManager) {
        this.brewProperties = brewProperties;
        this.observationRegistry = observationRegistry;
        this.presentingServiceClient = presentingServiceClient;
        this.bottlingService = bottlingService;
        this.restTemplate = restTemplate;
        this.eventGateway = eventGateway;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.baggageManager = baggageManager;
    }

    @Async
    public void updateBottlingServiceAboutBrewedBeer(final Ingredients ingredients, String processId) {
        Observation.createNotStarted("inside_maturing", observationRegistry).observe(() -> {
            log.info("Updating bottling service. Current process id is equal [{}]", processId);
            notifyPresentingService(processId);
            brewBeer();
            eventGateway.emitEvent(Event.builder().eventType(EventType.BEER_MATURED).processId(processId).build());
            notifyBottlingService(ingredients, processId);
        });
    }

    private void brewBeer() {
        try {
            Long timeout = brewProperties.getTimeout();
            log.info("Brewing beer... it will take [{}] ms", timeout);
            Thread.sleep(timeout);
        }
        catch (InterruptedException e) {
            log.error("Exception occurred while brewing beer", e);
        }
    }

    private void notifyPresentingService(String correlationId) {
        Observation.createNotStarted("inside_maturing", observationRegistry).observe(() -> {
            log.info("Calling presenting from maturing");
            String testCommunicationType = TestCommunication.fromBaggage(baggageManager);
            log.info("Found the following communication type [{}]", testCommunicationType);
            switch (testCommunicationType) {
            case "FEIGN":
                callPresentingViaFeign(correlationId);
                break;
            default:
                useRestTemplateToCallPresenting(correlationId);
            }
        });
    }

    private void callPresentingViaFeign(String correlationId) {
        presentingServiceClient.maturingFeed(correlationId, "FEIGN");
    }

    public void notifyBottlingService(Ingredients ingredients, String correlationId) {
        circuitBreakerFactory.create("notifyBottlingService").run(() -> {
            Observation.createNotStarted("calling_bottling_from_maturing", observationRegistry).observe(() -> {
                log.info("Calling bottling from maturing");
                bottlingService.bottle(new Wort(getQuantity(ingredients)), correlationId, "FEIGN");
            });
            return null;
        });
    }

    private void useRestTemplateToCallPresenting(String processId) {
        log.info("Calling presenting - process id [{}]", processId);
        restTemplate.exchange(requestEntity()
                .processId(processId)
                .contentTypeVersion(Version.PRESENTING_V1)
                .serviceName(Collaborators.PRESENTING)
                .url("feed/maturing")
                .httpMethod(HttpMethod.PUT)
                .build(), String.class);
    }

    private Integer getQuantity(Ingredients ingredients) {
        Assert.notEmpty(ingredients.ingredients, "Ingredients can't be null");
        return ingredients.ingredients.get(0).getQuantity();
    }

}

