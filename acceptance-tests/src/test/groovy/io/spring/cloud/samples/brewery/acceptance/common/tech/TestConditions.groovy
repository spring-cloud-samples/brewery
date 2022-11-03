package io.spring.cloud.samples.brewery.acceptance.common.tech

import io.spring.cloud.samples.brewery.acceptance.common.WhatToTest
import org.junit.jupiter.api.Assumptions

class TestConditions {

	private static String getAndLogWhatToTestSystemProp() {
		String whatToTestProp = System.getProperty(WhatToTest.WHAT_TO_TEST_SYSTEM_PROP) ?:
				System.getenv(WhatToTest.WHAT_TO_TEST_SYSTEM_PROP)
		println "WHAT_TO_TEST system prop equals [$whatToTestProp]"
		if (!whatToTestProp) {
			throw new WhatToTestSystemPropertyMissingException("You have to provide the WHAT_TO_TEST system property! " +
					"It can have one of these values ${WhatToTest.values()}")
		}
		return whatToTestProp.trim()
	}

	public static final Closure<Boolean> SERVICE_DISCOVERY = {
		return whatToTestSystemPropMatchesAny(
				[WhatToTest.CONSUL, WhatToTest.EUREKA, WhatToTest.ZOOKEEPER, WhatToTest.WAVEFRONT]
		)
	}

	public static final Closure<Boolean> SYSTEM_PROP_IS_VALID = {
		return whatToTestSystemPropMatchesAny(WhatToTest.values().toList())
	}

	static void assumeServiceDiscovery() {
		assumeSystemPropIsValid()
		Assumptions.assumeTrue(SERVICE_DISCOVERY())
	}

	static void assumeNotWavefront() {
		assumeSystemPropIsValid()
		String whatToTest = getAndLogWhatToTestSystemProp()
		Assumptions.assumeFalse(whatToTest.equalsIgnoreCase(WhatToTest.WAVEFRONT.name()))
	}

	static void assumeSystemPropIsValid() {
		Assumptions.assumeTrue(whatToTestSystemPropMatchesAny(WhatToTest.values().toList()))
	}

	static void assumeSystemPropIsInValid() {
		Assumptions.assumeFalse(whatToTestSystemPropMatchesAny(WhatToTest.values().toList()))
	}

	private static boolean whatToTestSystemPropMatchesAny(List<WhatToTest> whatToTest) {
		String whatToTestProp = getAndLogWhatToTestSystemProp()
		return whatToTest.any {
			it.toString().equalsIgnoreCase(whatToTestProp)
		}
	}

	static class WhatToTestSystemPropertyMissingException extends RuntimeException {
		WhatToTestSystemPropertyMissingException(String msg) {
			super(msg)
		}
	}
}
