package io.spring.cloud.samples.brewery.bottling;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.TestConfiguration;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

@Configuration
@Import(TestConfiguration.class)
class BottlingConfiguration {

    @Bean
    BottlerService bottlingService(BottlingWorker bottlingWorker,
            PresentingClient presentingClient,
            ObservationRegistry observationRegistry, CircuitBreakerFactory circuitBreakerFactory, @LoadBalanced RestTemplate restTemplate, BaggageManager baggageManager) {
        return new BottlerService(bottlingWorker, presentingClient, restTemplate, observationRegistry, circuitBreakerFactory, baggageManager);
    }

    @Bean
    BottlingWorker bottlingWorker(ObservationRegistry observationRegistry,
            PresentingClient presentingClient,
            @LoadBalanced RestTemplate restTemplate, EventGateway eventGateway, BaggageManager baggageManager) {
        return new BottlingWorker(observationRegistry, presentingClient, restTemplate, eventGateway, baggageManager);
    }

    @Bean
    Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry, Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
        return new Resilience4JCircuitBreakerFactory(circuitBreakerRegistry, timeLimiterRegistry, null, resilience4JConfigurationProperties);
    }

    // [Observability] instrumenting executors
    @Bean(destroyMethod = "shutdown")
    @Primary
    ThreadPoolTaskScheduler bottlingThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler() {
            @Override
            protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
                ExecutorService executorService = super.initializeExecutor(threadFactory, rejectedExecutionHandler);
                return ContextSnapshot.captureAll().wrapExecutorService(executorService);
            }
        };
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }
}
