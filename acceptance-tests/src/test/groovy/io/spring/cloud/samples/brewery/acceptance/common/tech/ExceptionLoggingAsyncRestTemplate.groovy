package io.spring.cloud.samples.brewery.acceptance.common.tech

import groovy.util.logging.Slf4j
import org.springframework.http.HttpMethod
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.web.client.AsyncRequestCallback
import org.springframework.web.client.AsyncRestTemplate
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestClientException

@Slf4j
class ExceptionLoggingAsyncRestTemplate extends AsyncRestTemplate {

	ExceptionLoggingAsyncRestTemplate() {
		errorHandler = new ExceptionLoggingErrorHandler()
	}

	@Override
	protected <T> ListenableFuture<T> doExecute(URI url, HttpMethod method, AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
		try {
			super.doExecute(url, method, requestCallback, responseExtractor)
		} catch (Exception e) {
			log.error("Exception occurred while trying to send a request", e)
			throw new AssertionError(e)
		}
	}
}
