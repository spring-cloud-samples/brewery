package io.spring.cloud.samples.brewery.bottling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Version;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;
import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

@Component
@Slf4j
class BottlingWorker {


    private Map<String, State> PROCESS_STATE = new ConcurrentHashMap<>();
    private final Tracer tracer;
    private final PresentingClient presentingClient;
    private final RestTemplate restTemplate;
    private final EventGateway eventGateway;

    @Autowired
    public BottlingWorker(Tracer tracer,
                          PresentingClient presentingClient,
                          @LoadBalanced RestTemplate restTemplate, EventGateway eventGateway) {
        this.tracer = tracer;
        this.presentingClient = presentingClient;
        this.restTemplate = restTemplate;
        this.eventGateway = eventGateway;
    }

    @Async
    public void bottleBeer(Integer wortAmount, String processId, TestConfigurationHolder configurationHolder) {
        TestConfigurationHolder.TEST_CONFIG.set(configurationHolder);
        increaseBottles(wortAmount, processId);
        eventGateway.emitEvent(Event.builder().eventType(EventType.BEER_BOTTLED).processId(processId).build());
        notifyPresentingService(processId);
    }

    private void notifyPresentingService(String processId) {
        Span scope = this.tracer.startTrace("calling_presenting");
        switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callPresentingViaFeign(processId);
                break;
            default:
                useRestTemplateToCallPresenting(processId);
        }
        tracer.close(scope);
    }

    private void increaseBottles(Integer wortAmount, String processId) {
        log.info("Bottling beer...");
        Span scope = tracer.startTrace("waiting_for_beer_bottling");
        try {
            State stateForProcess = PROCESS_STATE.getOrDefault(processId, new State());
            Integer bottled = stateForProcess.bottled;
            Integer bottles = stateForProcess.bottles;
            int bottlesCount = wortAmount / 10;
            bottled += bottlesCount;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // i love this construct
            }
            bottles += bottlesCount;
            bottled -= bottlesCount;
            stateForProcess.setBottled(bottled);
            stateForProcess.setBottles(bottles);
            PROCESS_STATE.put(processId, stateForProcess);
        } finally {
            tracer.close(scope);
        }
    }

    private void callPresentingViaFeign(String processId) {
        presentingClient.updateBottles(PROCESS_STATE.get(processId).getBottles(), processId, FEIGN.name());
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

    @Data
    private static class State {
        private Integer bottles = 0;
        private Integer bottled = 0;
    }
}
