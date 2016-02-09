package io.spring.cloud.samples.brewery.common;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;

public class TestRequestEntityBuilder {
	private static final String PROCESS_ID_HEADER = "PROCESS-ID";
	private static final String CONTENT_TYPE_HEADER = "Content-Type";

	private String processId;
	private TestConfigurationHolder.TestCommunicationType testCommunicationType;
	private String serviceName;
	private String url;
	private String version;
	private HttpMethod httpMethod;
	private Object body = "Some body";

	public static TestRequestEntityBuilder requestEntity() {
		return new TestRequestEntityBuilder();
	}

	public TestRequestEntityBuilder processId(String processId) {
		this.processId = processId;
		return this;
	}

	public TestRequestEntityBuilder setTestCommunicationType(TestConfigurationHolder.TestCommunicationType testCommunicationType) {
		this.testCommunicationType = testCommunicationType;
		return this;
	}

	public TestRequestEntityBuilder serviceName(String serviceName) {
		this.serviceName = serviceName;
		return this;
	}

	public TestRequestEntityBuilder url(String url) {
		this.url = url;
		return this;
	}

	public TestRequestEntityBuilder contentTypeVersion(String version) {
		this.version = version;
		return this;
	}

	public TestRequestEntityBuilder httpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
		return this;
	}

	public TestRequestEntityBuilder body(Object body) {
		this.body = body;
		return this;
	}

	public RequestEntity build() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(PROCESS_ID_HEADER, processId);
		headers.add(CONTENT_TYPE_HEADER, version);
		headers.add(TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME, getCommunicationTypeHeader());
		URI uri = URI.create("http://" + serviceName + "/" + url);
		return new RequestEntity<>(body, headers, httpMethod, uri);
	}

	private String getCommunicationTypeHeader() {
		if (testCommunicationType != null) {
			return testCommunicationType.name();
		} else if (TestConfigurationHolder.TEST_CONFIG.get() == null) {
			return TestConfigurationHolder.TestCommunicationType.REST_TEMPLATE.name();
		} else if (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType() != null) {
			return TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType().name();
		}
		return TestConfigurationHolder.TestCommunicationType.REST_TEMPLATE.name();
	}
}