package io.spring.cloud.samples.brewery.maturing;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.maturing.model.Ingredients;
import io.spring.cloud.samples.brewery.maturing.model.Version;
import io.spring.cloud.samples.brewery.maturing.model.Wort;
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
    private final BottlingServiceClient bottlingServiceClient;
    private final RestTemplate restTemplate;

    public BottlingServiceUpdater(BrewProperties brewProperties,
                                  TraceManager traceManager,
                                  PresentingServiceClient presentingServiceClient,
                                  BottlingServiceClient bottlingServiceClient,
                                  RestTemplate restTemplate) {
        this.brewProperties = brewProperties;
        this.traceManager = traceManager;
        this.presentingServiceClient = presentingServiceClient;
        this.bottlingServiceClient = bottlingServiceClient;
        this.restTemplate = restTemplate;
    }

    @Async
    public void updateBottlingServiceAboutBrewedBeer(final Ingredients ingredients, String processId, TestConfigurationHolder configurationHolder) {
        TestConfigurationHolder.TEST_CONFIG.set(configurationHolder);
        log.info("Current trace id is equal [{}]", processId);
        notifyPresentingService(processId);
        try {
            Long timeout = brewProperties.getTimeout();
            log.info("Brewing beer... it will take [{}] ms", timeout);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            log.error("Exception occurred while brewing beer", e);
        }
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

    private void notifyBottlingService(Ingredients ingredients, String correlationId) {
        Trace scope = this.traceManager.startSpan("calling_bottling", TraceContextHolder.getCurrentSpan());
        switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callBottlingViaFeign(ingredients, correlationId);
                break;
            default:
                useRestTemplateToCallBottling(new Wort(getQuantity(ingredients)), correlationId);
        }
        traceManager.close(scope);
    }

    private void useRestTemplateToCallPresenting(String processId) {
        log.info("Calling presenting - process id [{}]", processId);
        restTemplate.exchange(requestEntity()
                .processId(processId)
                .contentTypeVersion(Version.PRESENTING_V1)
                .serviceName("presenting")
                .url("feed/maturing")
                .httpMethod(HttpMethod.PUT)
                .build(), String.class);
    }

    private void callBottlingViaFeign(Ingredients ingredients, String correlationId) {
        bottlingServiceClient.bottle(new Wort(getQuantity(ingredients)), correlationId, FEIGN.name());
    }

    private void useRestTemplateToCallBottling(Wort wort, String processId) {
        log.info("Calling bottling - process id [{}]", processId);
        restTemplate.exchange(requestEntity()
                .processId(processId)
                .contentTypeVersion(Version.BOTTLING_V1)
                .serviceName("bottling")
                .url("bottle")
                .httpMethod(HttpMethod.POST)
                .body(wort)
                .build(), String.class);
    }

    private Integer getQuantity(Ingredients ingredients) {
        Assert.notEmpty(ingredients.ingredients);
        return ingredients.ingredients.get(0).getQuantity();
    }

}

