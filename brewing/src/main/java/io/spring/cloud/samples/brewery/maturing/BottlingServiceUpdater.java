package io.spring.cloud.samples.brewery.maturing;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.spring.cloud.samples.brewery.common.BottlingService;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.model.Version;
import io.spring.cloud.samples.brewery.common.model.Wort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Trace;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;
import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

@Slf4j
class BottlingServiceUpdater {

    private final BrewProperties brewProperties;
    private final TraceManager traceManager;
    private final PresentingServiceClient presentingServiceClient;
    private final BottlingService bottlingService;
    private final RestTemplate restTemplate;
    private final EventGateway eventGateway;

    public BottlingServiceUpdater(BrewProperties brewProperties,
                                  TraceManager traceManager,
                                  PresentingServiceClient presentingServiceClient,
                                  BottlingService bottlingService,
                                  RestTemplate restTemplate, EventGateway eventGateway) {
        this.brewProperties = brewProperties;
        this.traceManager = traceManager;
        this.presentingServiceClient = presentingServiceClient;
        this.bottlingService = bottlingService;
        this.restTemplate = restTemplate;
        this.eventGateway = eventGateway;
    }

    @Async
    public void updateBottlingServiceAboutBrewedBeer(final Ingredients ingredients, String processId, TestConfigurationHolder configurationHolder) {
        TestConfigurationHolder.TEST_CONFIG.set(configurationHolder);
        log.info("Current process id is equal [{}]. Span is [{}]", processId, TraceContextHolder.isTracing() ?
            TraceContextHolder.getCurrentSpan() : "");
        notifyPresentingService(processId);
        try {
            Long timeout = brewProperties.getTimeout();
            log.info("Brewing beer... it will take [{}] ms", timeout);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            log.error("Exception occurred while brewing beer", e);
        }
        eventGateway.emitEvent(Event.builder().eventType(EventType.BEER_MATURED).processId(processId).build());
        notifyBottlingService(ingredients, processId);
    }

    private void notifyPresentingService(String correlationId) {
        Trace scope = this.traceManager.startSpan("calling_presenting", TraceContextHolder.getCurrentSpan());
        switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callPresentingViaFeign(correlationId);
                break;
            default:
                useRestTemplateToCallPresenting(correlationId);
        }
        traceManager.close(scope);
    }

    private void callPresentingViaFeign(String correlationId) {
        presentingServiceClient.maturingFeed(correlationId, FEIGN.name());
    }

	/**
     * [SLEUTH] HystrixCommand - Javanica integration
     */
    @HystrixCommand
    public void notifyBottlingService(Ingredients ingredients, String correlationId) {
        Trace scope = this.traceManager.startSpan("calling_bottling", TraceContextHolder.getCurrentSpan());
        bottlingService.bottle(new Wort(getQuantity(ingredients)), correlationId, FEIGN.name());
        traceManager.close(scope);
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
        Assert.notEmpty(ingredients.ingredients);
        return ingredients.ingredients.get(0).getQuantity();
    }

}

