package io.spring.cloud.samples.brewery.acceptance.common;

public enum WhatToTest {
	SLEUTH, ZOOKEEPER, CONSUL, EUREKA, SCS;

	public static final String WHAT_TO_TEST_SYSTEM_PROP = "WHAT_TO_TEST";
}
