package io.spring.cloud.samples.brewery.bottling;

import java.net.URI;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import io.spring.cloud.samples.brewery.common.model.Version;
import io.spring.cloud.samples.brewery.common.model.Wort;
import lombok.extern.slf4j.Slf4j;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TEST_CONFIG;
import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;
import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

@Slf4j
class BottlerService {

    private final BottlingWorker bottlingWorker;
    private final PresentingClient presentingClient;
    private final RestTemplate restTemplate;
    private final AsyncRestTemplate asyncRestTemplate;
    private final Tracer tracer;

    public BottlerService(BottlingWorker bottlingWorker, PresentingClient presentingClient,
                          RestTemplate restTemplate, AsyncRestTemplate asyncRestTemplate, Tracer tracer) {
        this.bottlingWorker = bottlingWorker;
        this.presentingClient = presentingClient;
        this.restTemplate = restTemplate;
        this.asyncRestTemplate = asyncRestTemplate;
        this.tracer = tracer;
    }

    /**
     * [SLEUTH] HystrixCommand - Javanica integration
     */
    @HystrixCommand
    void bottle(Wort wort, String processId) {
        log.info("I'm inside bottling");
        Span span = tracer.startTrace("local:inside_bottling");
        try {
            notifyPresenting(processId);
            bottlingWorker.bottleBeer(wort.getWort(), processId, TEST_CONFIG.get());
        } finally {
            tracer.close(span);
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
        URI uri = URI.create("http://presenting/feed/bottling");
        asyncRestTemplate.put(uri, requestEntity);
    }
}
