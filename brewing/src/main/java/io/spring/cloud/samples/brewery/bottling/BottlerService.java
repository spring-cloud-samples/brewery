package io.spring.cloud.samples.brewery.bottling;

import java.net.URI;

import brave.Span;
import brave.Tracer;
import brave.baggage.BaggageField;
import brave.propagation.ExtraFieldPropagation;
import io.spring.cloud.samples.brewery.common.model.Version;
import io.spring.cloud.samples.brewery.common.model.Wort;
import org.slf4j.Logger;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

class BottlerService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BottlerService.class);
    private final BottlingWorker bottlingWorker;
    private final PresentingClient presentingClient;
    private final RestTemplate restTemplate;
    private final Tracer tracer;
    private final CircuitBreakerFactory factory;

    public BottlerService(BottlingWorker bottlingWorker, PresentingClient presentingClient,
                          RestTemplate restTemplate, Tracer tracer, CircuitBreakerFactory factory) {
        this.bottlingWorker = bottlingWorker;
        this.presentingClient = presentingClient;
        this.restTemplate = restTemplate;
        this.tracer = tracer;
        this.factory = factory;
    }

    void bottle(Wort wort, String processId) {
        factory.create("bottle").run(() -> {
            log.info("I'm inside bottling");
            Span span = tracer.nextSpan().name("inside_bottling").start();
            try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
                bottleWithCircuitBreaker(wort, processId);
            } finally {
                span.finish();
            }
            return null;
        });
    }

    /**
     * [OBSERVABILITY] CircuitBreaker integration
     */
    void bottleWithCircuitBreaker(Wort wort, String processId) {
        Span span = tracer.nextSpan().name("inside_bottling_circuitbreaker").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            notifyPresenting(processId);
            factory.create("circuitbreaker").run(() -> {
                bottlingWorker.bottleBeer(wort.getWort(), processId);
                return null;
            });
        } finally {
            span.finish();
        }
    }

    void notifyPresenting(String processId) {
        log.info("I'm inside bottling. Notifying presenting");
        String testCommunicationType = BaggageField.getByName("TEST-COMMUNICATION-TYPE").getValue();
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

	/**
     * [OBSERVABILITY] AsyncRestTemplate with sync @LoadBalanced RestTemplate
     */
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
