package io.spring.cloud.samples.brewery.acceptance;

import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConditions;
import org.junit.jupiter.api.Test;

/**
 * @author Marcin Grzejszczak
 */
public class SystemPropertyValidityTests {
	@Test
	public void will_fail_if_WHAT_TO_TEST_system_property_has_not_been_passed_or_is_invalid() {
		TestConditions.assumeSystemPropIsInValid();
		throw new WhatToTestSystemPropertyNotPassedOrIsWrongException();
	}

	static class WhatToTestSystemPropertyNotPassedOrIsWrongException extends RuntimeException {
		WhatToTestSystemPropertyNotPassedOrIsWrongException() {
			super("System property 'WHAT_TO_TEST' is equal [${System.getProperty('WHAT_TO_TEST')}]. Valid entries" +
					" are ${WhatToTest.values()}");
		}
	}
}
