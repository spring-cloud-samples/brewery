package io.spring.cloud.samples.brewery.bottling;

import java.net.URI;

import brave.Span;
import brave.Tracer;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.model.Version;
import io.spring.cloud.samples.brewery.common.model.Wort;
import org.slf4j.Logger;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TEST_CONFIG;
import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;
import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

class BottlerService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BottlerService.class);
    private final BottlingWorker bottlingWorker;
    private final PresentingClient presentingClient;
    private final RestTemplate restTemplate;
    private final AsyncRestTemplate asyncRestTemplate;
    private final Tracer tracer;
    private final CircuitBreakerFactory factory;

    public BottlerService(BottlingWorker bottlingWorker, PresentingClient presentingClient,
                          RestTemplate restTemplate, AsyncRestTemplate asyncRestTemplate, Tracer tracer, CircuitBreakerFactory factory) {
        this.bottlingWorker = bottlingWorker;
        this.presentingClient = presentingClient;
        this.restTemplate = restTemplate;
        this.asyncRestTemplate = asyncRestTemplate;
        this.tracer = tracer;
        this.factory = factory;
    }

    /**
     * [SLEUTH] HystrixCommand - Javanica integration
     */
    @HystrixCommand
    void bottle(Wort wort, String processId) {
        log.info("I'm inside bottling");
        Span span = tracer.nextSpan().name("inside_bottling").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
           bottleWithCircuitBreaker(wort, processId);
        } finally {
            span.finish();
        }
    }

    /**
     * [SLEUTH] CircuitBreaker integration
     */
    void bottleWithCircuitBreaker(Wort wort, String processId) {
        Span span = tracer.nextSpan().name("inside_bottling_circuitbreaker").start();
        TestConfigurationHolder testConfigurationHolder = TEST_CONFIG.get();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            notifyPresenting(processId);
            factory.create("circuitbreaker").run(() -> {
                bottlingWorker.bottleBeer(wort.getWort(), processId, testConfigurationHolder);
                return null;
            });
        } finally {
            span.finish();
        }
    }

    void notifyPresenting(String processId) {
        log.info("I'm inside bottling. Notifying presenting");
        switch (TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callPresentingViaFeign(processId);
                break;
            default:
                useRestTemplateToCallPresenting(processId);
        }
    }

    private void callPresentingViaFeign(String processId) {
        presentingClient.bottlingFeed(processId, FEIGN.name());
    }

	/**
     * [SLEUTH] AsyncRestTemplate with sync @LoadBalanced RestTemplate
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
        URI uri = URI.create("https://presenting/feed/bottling");
        asyncRestTemplate.put(uri, requestEntity);
    }
}
