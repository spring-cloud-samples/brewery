package io.spring.cloud.samples.brewery.acceptance

import io.spring.cloud.samples.brewery.acceptance.common.WhatToTest
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConditions
import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ TestConditions.SYSTEM_PROP_IS_VALID() })
class SystemPropertyValiditySpec extends Specification {

	def "will fail if WHAT_TO_TEST system property has not been passed or is invalid"() {
		expect:
			throw new WhatToTestSystemPropertyNotPassedOrIsWrongException()
	}

	static class WhatToTestSystemPropertyNotPassedOrIsWrongException extends RuntimeException {
		WhatToTestSystemPropertyNotPassedOrIsWrongException() {
			super("System property 'WHAT_TO_TEST' is equal [${System.getProperty('WHAT_TO_TEST')}]. Valid entries" +
					" are ${WhatToTest.values()}")
		}
	}
}
