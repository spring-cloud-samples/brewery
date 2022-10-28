package io.spring.cloud.samples.brewery.common;

import io.micrometer.tracing.BaggageManager;

import java.util.Objects;

public class TestCommunication {

    public static String fromBaggage(BaggageManager baggageManager) {
        return Objects.requireNonNull(baggageManager.getBaggage("TEST-COMMUNICATION-TYPE"), "Baggage wasn't properly propagated!").get();
    }
}
