package io.spring.cloud.samples.brewery.maturing;

import brave.Span;
import brave.Tracer;
import brave.baggage.BaggageField;
import brave.propagation.ExtraFieldPropagation;
import io.spring.cloud.samples.brewery.common.BottlingService;
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
    private final Tracer tracer;
    private final PresentingServiceClient presentingServiceClient;
    private final BottlingService bottlingService;
    private final RestTemplate restTemplate;
    private final EventGateway eventGateway;
    private final CircuitBreakerFactory circuitBreakerFactory;

    public BottlingServiceUpdater(BrewProperties brewProperties,
            Tracer tracer,
            PresentingServiceClient presentingServiceClient,
            BottlingService bottlingService,
            RestTemplate restTemplate, EventGateway eventGateway, CircuitBreakerFactory circuitBreakerFactory) {
        this.brewProperties = brewProperties;
        this.tracer = tracer;
        this.presentingServiceClient = presentingServiceClient;
        this.bottlingService = bottlingService;
        this.restTemplate = restTemplate;
        this.eventGateway = eventGateway;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Async
    public void updateBottlingServiceAboutBrewedBeer(final Ingredients ingredients, String processId) {
        Span trace = tracer.nextSpan().name("inside_maturing").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(trace)) {
            log.info("Updating bottling service. Current process id is equal [{}]", processId);
            notifyPresentingService(processId);
            brewBeer();
            eventGateway.emitEvent(Event.builder().eventType(EventType.BEER_MATURED).processId(processId).build());
            notifyBottlingService(ingredients, processId);
        } finally {
            trace.finish();
        }
    }

    private void brewBeer() {
        try {
			Long timeout = brewProperties.getTimeout();
			log.info("Brewing beer... it will take [{}] ms", timeout);
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
			log.error("Exception occurred while brewing beer", e);
		}
    }

    private void notifyPresentingService(String correlationId) {
        log.info("Calling presenting from maturing");
        Span scope = this.tracer.nextSpan().name("calling_presenting_from_maturing").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(scope)) {
            String testCommunicationType = BaggageField.getByName("TEST-COMMUNICATION-TYPE").getValue();
            log.info("Found the following communication type [{}]", testCommunicationType);
            switch (testCommunicationType) {
            case "FEIGN":
                callPresentingViaFeign(correlationId);
                break;
            default:
                useRestTemplateToCallPresenting(correlationId);
            }
        } finally {
            scope.finish();
        }
    }

    private void callPresentingViaFeign(String correlationId) {
        presentingServiceClient.maturingFeed(correlationId, "FEIGN");
    }

    public void notifyBottlingService(Ingredients ingredients, String correlationId) {
        circuitBreakerFactory.create("notifyBottlingService").run(() -> {
            log.info("Calling bottling from maturing");
            Span scope = this.tracer.nextSpan().name("calling_bottling_from_maturing").start();
            try (Tracer.SpanInScope ws = tracer.withSpanInScope(scope)) {
                bottlingService.bottle(new Wort(getQuantity(ingredients)), correlationId, "FEIGN");
            } finally {
                scope.finish();
            }
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

