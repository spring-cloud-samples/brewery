package io.spring.cloud.samples.brewery.common;

import java.util.Objects;

import io.micrometer.tracing.BaggageManager;

public class TestCommunication {

	public static String fromBaggage(BaggageManager baggageManager) {
		return Objects.requireNonNull(baggageManager.getBaggage("TEST-COMMUNICATION-TYPE"), "Baggage wasn't properly propagated!").get();
	}
}
