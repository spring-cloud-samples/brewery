package io.spring.cloud.samples.brewery.maturing;

import io.spring.cloud.samples.brewery.common.TestConfiguration;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(TestConfiguration.class)
class BrewConfiguration {

    @Bean
    BottlingServiceUpdater bottlingServiceUpdater(TraceManager trace, PresentingServiceClient presentingServiceClient,
                                                  BottlingServiceClient bottlingServiceClient,
                                                  @LoadBalanced RestTemplate restTemplate,
                                                  EventGateway eventGateway) {
        return new BottlingServiceUpdater(brewProperties(), trace, presentingServiceClient,
                bottlingServiceClient, restTemplate, eventGateway);
    }

    @Bean
    BrewProperties brewProperties() {
        return new BrewProperties();
    }

}

