package io.spring.cloud.samples.brewery.acceptance.common.tech

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
@EnableAutoConfiguration
class TestConfiguration {

	@Bean
	ServiceUrlFetcher serviceUrlFetcher(Environment environment) {
		return new ServiceUrlFetcher(environment)
	}

}
