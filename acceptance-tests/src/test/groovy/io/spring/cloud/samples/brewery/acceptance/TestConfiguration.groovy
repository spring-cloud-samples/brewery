package io.spring.cloud.samples.brewery.acceptance

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

import javax.annotation.PostConstruct

@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
class TestConfiguration {

	@Autowired @LoadBalanced RestTemplate restTemplate

	@PostConstruct
	void customizeRestTemplate() {
		this.restTemplate.errorHandler = new ExceptionLoggingErrorHandler()
	}
}
