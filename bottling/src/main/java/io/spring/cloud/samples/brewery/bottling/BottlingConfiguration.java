package io.spring.cloud.samples.brewery.bottling;

import io.spring.cloud.samples.brewery.common.TestConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(TestConfiguration.class)
class BottlingConfiguration {

    @Bean
    BottlerService bottlingService(BottlingWorker bottlingWorker,
                                   PresentingClient presentingClient,
                                   @LoadBalanced RestTemplate restTemplate) {
        return new BottlerService(bottlingWorker, presentingClient, restTemplate);
    }

}

