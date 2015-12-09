package io.spring.cloud.samples.brewery.acceptance

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

import javax.annotation.PostConstruct

@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
@Slf4j
class TestConfiguration {

	@Autowired @LoadBalanced RestTemplate restTemplate

	@PostConstruct
	void customizeRestTemplate() {
		this.restTemplate.errorHandler = new DefaultResponseErrorHandler() {
			@Override
			void handleError(ClientHttpResponse response) throws IOException {
				if (hasError(response)) {
					log.error("Response has status code [${response.statusCode}] and text [${response.statusText}]")
				}
			}
		}
	}
}
