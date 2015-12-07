package io.spring.cloud.samples.brewery.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestConfigurationHolder {
	public static final String TEST_COMMUNICATION_TYPE_HEADER_NAME = "TEST-COMMUNICATION-TYPE";
	public static final ThreadLocal<TestConfigurationHolder> TEST_CONFIG = new ThreadLocal<>();

	private TestCommunicationType testCommunicationType;

	public enum TestCommunicationType {
		FEIGN, REST_TEMPLATE
	}
}
