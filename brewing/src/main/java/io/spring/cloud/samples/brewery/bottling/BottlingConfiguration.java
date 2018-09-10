package io.spring.cloud.samples.brewery.bottling;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import brave.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import io.spring.cloud.samples.brewery.common.TestConfiguration;

@Configuration
@Import(TestConfiguration.class)
class BottlingConfiguration {

    @Bean
    BottlerService bottlingService(BottlingWorker bottlingWorker,
                                   PresentingClient presentingClient,
                                   @LoadBalanced RestTemplate restTemplate
                                   Tracer tracer) {
        return new BottlerService(bottlingWorker, presentingClient,
            restTemplate, bottlingAsyncRestTemplate(restTemplate), tracer);
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
    ThreadPoolTaskScheduler bottlingThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }
}
