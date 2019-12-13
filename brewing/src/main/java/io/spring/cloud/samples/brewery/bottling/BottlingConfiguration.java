package io.spring.cloud.samples.brewery.bottling;

import brave.Tracer;
import io.spring.cloud.samples.brewery.common.TestConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(TestConfiguration.class)
class BottlingConfiguration {

    @Bean
    BottlerService bottlingService(BottlingWorker bottlingWorker,
                                   PresentingClient presentingClient,
                                   @LoadBalanced RestTemplate restTemplate,
                                   Tracer tracer, CircuitBreakerFactory circuitBreakerFactory) {
        return new BottlerService(bottlingWorker, presentingClient,
            restTemplate, bottlingAsyncRestTemplate(restTemplate), tracer, circuitBreakerFactory);
    }

    @Bean
    Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory() {
        return new Resilience4JCircuitBreakerFactory();
    }

    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean
    public RestTemplate bottlingLoadBalancedRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    AsyncRestTemplate bottlingAsyncRestTemplate(@LoadBalanced RestTemplate restTemplate) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setTaskExecutor(bottlingThreadPoolTaskScheduler());
        return new AsyncRestTemplate(requestFactory, restTemplate);
    }

    @Bean(destroyMethod = "shutdown")
    @Primary
    ThreadPoolTaskScheduler bottlingThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }
}
