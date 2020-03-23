package io.spring.cloud.samples.brewery.maturing;

import brave.Tracer;
import io.spring.cloud.samples.brewery.common.BottlingService;
import io.spring.cloud.samples.brewery.common.TestConfiguration;
import io.spring.cloud.samples.brewery.common.events.EventGateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(TestConfiguration.class)
class BrewConfiguration {

    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean
    public RestTemplate brewLoadBalancedRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    BottlingServiceUpdater bottlingServiceUpdater(Tracer trace, PresentingServiceClient presentingServiceClient,
                                                  BottlingService bottlingService,
                                                  @LoadBalanced RestTemplate restTemplate,
                                                  EventGateway eventGateway, CircuitBreakerFactory circuitBreakerFactory) {
        return new BottlingServiceUpdater(brewProperties(), trace, presentingServiceClient,
                bottlingService, restTemplate, eventGateway, circuitBreakerFactory);
    }

    @Bean
    BrewProperties brewProperties() {
        return new BrewProperties();
    }

}
