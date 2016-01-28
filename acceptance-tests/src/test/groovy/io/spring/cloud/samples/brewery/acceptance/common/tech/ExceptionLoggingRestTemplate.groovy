package io.spring.cloud.samples.brewery.acceptance.common.tech

import groovy.util.logging.Slf4j
import org.springframework.http.HttpMethod
import org.springframework.web.client.RequestCallback
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Slf4j
class ExceptionLoggingRestTemplate extends RestTemplate {

	ExceptionLoggingRestTemplate() {
		errorHandler = new ExceptionLoggingErrorHandler()
	}

	@Override
	protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
		try {
			return super.doExecute(url, method, requestCallback, responseExtractor)
		} catch (Exception e) {
			log.error("Exception occurred while trying to send a request", e)
			throw new AssertionError(e)
		}
	}
}
