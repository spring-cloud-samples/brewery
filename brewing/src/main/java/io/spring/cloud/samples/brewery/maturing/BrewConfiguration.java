package io.spring.cloud.samples.brewery.maturing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.BottlingService;
import io.spring.cloud.samples.brewery.common.TestConfiguration;
import io.spring.cloud.samples.brewery.common.events.EventGateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(TestConfiguration.class)
@EnableConfigurationProperties(BrewProperties.class)
class BrewConfiguration {

	@Bean
	BottlingServiceUpdater bottlingServiceUpdater(ObservationRegistry observationRegistry, PresentingServiceClient presentingServiceClient,
		BottlingService bottlingService, @LoadBalanced RestTemplate restTemplate,
		EventGateway eventGateway, CircuitBreakerFactory circuitBreakerFactory, BaggageManager baggageManager, BrewProperties properties) {
		return new BottlingServiceUpdater(properties, observationRegistry, presentingServiceClient,
			bottlingService, restTemplate, eventGateway, circuitBreakerFactory, baggageManager);
	}

}
