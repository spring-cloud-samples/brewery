package io.spring.cloud.samples.brewery.acceptance.common.tech

import io.spring.cloud.samples.brewery.acceptance.common.WhatToTest

class TestConditions {
	public static final Closure<Boolean> WHAT_TO_TEST = { WhatToTest whatToTest ->
		return getAndLogWhatToTestSystemProp() == whatToTest.name()
	}

	private static String getAndLogWhatToTestSystemProp() {
		String whatToTestProp = System.getProperty(WhatToTest.WHAT_TO_TEST) ?:
				System.getenv(WhatToTest.WHAT_TO_TEST)
		println "WHAT_TO_TEST system prop equals [$whatToTestProp]"
		return whatToTestProp.trim()
	}

	public static final Closure<Boolean> SERVICE_DISCOVERY = {
		String whatToTestProp = getAndLogWhatToTestSystemProp()
		return [WhatToTest.CONSUL, WhatToTest.EUREKA, WhatToTest.ZOOKEEPER].any {
			it.toString().equalsIgnoreCase(whatToTestProp)
		}
	}
}
