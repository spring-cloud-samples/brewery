package io.spring.cloud.samples.brewery.acceptance.common.tech

import groovy.util.logging.Slf4j
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.DefaultResponseErrorHandler

@Slf4j
class ExceptionLoggingErrorHandler extends DefaultResponseErrorHandler {
	@Override
	void handleError(ClientHttpResponse response) throws IOException {
		if (hasError(response)) {
			log.error("Response has status code [${response.statusCode}] and text [${response.statusText}]")
		}
	}
}
