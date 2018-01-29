package io.spring.cloud.samples.brewery.bottling;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Version;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import brave.Span;
import brave.Tracer;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        Span scope = this.tracer.nextSpan().name("calling_presenting").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(scope)) {
            switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callPresentingViaFeign(processId);
                break;
            default:
                useRestTemplateToCallPresenting(processId);
            }
        } finally {
            scope.finish();
        }
    }

    private void increaseBottles(Integer wortAmount, String processId) {
        log.info("Bottling beer...");
        Span scope = tracer.nextSpan().name("waiting_for_beer_bottling").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(scope)) {
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
            scope.finish();
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
