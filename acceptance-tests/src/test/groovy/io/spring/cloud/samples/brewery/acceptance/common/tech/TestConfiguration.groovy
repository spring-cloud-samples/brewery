package io.spring.cloud.samples.brewery.acceptance.common.tech
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.client.RestTemplate

import javax.annotation.PostConstruct

@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
class TestConfiguration {

	@Autowired(required = false) @LoadBalanced RestTemplate loadBalanced

	@PostConstruct
	void customizeRestTemplate() {
		if (loadBalanced) {
			this.loadBalanced.errorHandler = new ExceptionLoggingErrorHandler()
		}
	}

	@Bean
	ServiceUrlFetcher serviceUrlFetcher(Environment environment) {
		return new ServiceUrlFetcher(environment)
	}

}
