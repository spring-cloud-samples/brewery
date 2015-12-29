package io.spring.cloud.samples.brewery.bottling;

import io.spring.cloud.samples.brewery.bottling.model.Version;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.Trace;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
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
public class BottlingWorker {


    private Map<String, State> PROCESS_STATE = new ConcurrentHashMap<>();
    private final TraceManager traceManager;
    private final PresentingClient presentingClient;
    private final RestTemplate restTemplate;
    private final EventGateway eventGateway;

    @Autowired
    public BottlingWorker(TraceManager traceManager,
                          PresentingClient presentingClient,
                          @LoadBalanced RestTemplate restTemplate, EventGateway eventGateway) {
        this.traceManager = traceManager;
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
        Trace scope = this.traceManager.startSpan("calling_presenting", TraceContextHolder.getCurrentSpan());
        switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callPresentingViaFeign(processId);
                break;
            default:
                useRestTemplateToCallPresenting(processId);
        }
        traceManager.close(scope);
    }

    private void increaseBottles(Integer wortAmount, String processId) {
        log.info("Bottling beer...");
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
    }

    private void callPresentingViaFeign(String processId) {
        presentingClient.updateBottles(PROCESS_STATE.get(processId).getBottles(), processId, FEIGN.name());
    }

    private void useRestTemplateToCallPresenting(String processId) {
        restTemplate.exchange(requestEntity()
                .processId(processId)
                .contentTypeVersion(Version.PRESENTING_V1)
                .serviceName("presenting")
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
