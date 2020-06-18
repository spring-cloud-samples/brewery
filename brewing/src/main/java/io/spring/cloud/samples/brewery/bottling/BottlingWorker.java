package io.spring.cloud.samples.brewery.bottling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import brave.Span;
import brave.Tracer;
import brave.baggage.BaggageField;
import brave.propagation.ExtraFieldPropagation;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Version;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

@Component
class BottlingWorker {


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BottlingWorker.class);
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
    public void bottleBeer(Integer wortAmount, String processId) {
        increaseBottles(wortAmount, processId);
        eventGateway.emitEvent(Event.builder().eventType(EventType.BEER_BOTTLED).processId(processId).build());
        notifyPresentingService(processId);
    }

    private void notifyPresentingService(String processId) {
        Span scope = this.tracer.nextSpan().name("calling_presenting").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(scope)) {
            String testCommunicationType = BaggageField.getByName("TEST-COMMUNICATION-TYPE").getValue();
            log.info("Found the following communication type [{}]", testCommunicationType);
            if (testCommunicationType.equals("FEIGN")) {
                callPresentingViaFeign(processId);
            }
            else {
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
        presentingClient.updateBottles(PROCESS_STATE.get(processId).getBottles(), processId, "FEIGN");
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

    private static class State {
        private Integer bottles = 0;
        private Integer bottled = 0;

        public State() {
        }

        public Integer getBottles() {
            return this.bottles;
        }

        public Integer getBottled() {
            return this.bottled;
        }

        public void setBottles(Integer bottles) {
            this.bottles = bottles;
        }

        public void setBottled(Integer bottled) {
            this.bottled = bottled;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof State)) return false;
            final State other = (State) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$bottles = this.getBottles();
            final Object other$bottles = other.getBottles();
            if (this$bottles == null ? other$bottles != null : !this$bottles.equals(other$bottles)) return false;
            final Object this$bottled = this.getBottled();
            final Object other$bottled = other.getBottled();
            if (this$bottled == null ? other$bottled != null : !this$bottled.equals(other$bottled)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof State;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $bottles = this.getBottles();
            result = result * PRIME + ($bottles == null ? 43 : $bottles.hashCode());
            final Object $bottled = this.getBottled();
            result = result * PRIME + ($bottled == null ? 43 : $bottled.hashCode());
            return result;
        }

        public String toString() {
            return "BottlingWorker.State(bottles=" + this.getBottles() + ", bottled=" + this.getBottled() + ")";
        }
    }
}
