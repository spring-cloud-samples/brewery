package io.spring.cloud.samples.brewery.common;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;

public class TestRequestEntityBuilder {
	private static final String PROCESS_ID_HEADER = "PROCESS-ID";
	private static final String CONTENT_TYPE_HEADER = "Content-Type";

	private String processId;
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
		URI uri = URI.create("http://" + serviceName + "/" + (url.startsWith("/") ? url.substring(1) : url));
		return new RequestEntity<>(body, headers, httpMethod, uri);
	}
}