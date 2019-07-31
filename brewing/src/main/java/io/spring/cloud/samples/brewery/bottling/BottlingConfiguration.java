package io.spring.cloud.samples.brewery.bottling;

import brave.Tracer;
import io.spring.cloud.samples.brewery.common.TestConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @Primary
    ThreadPoolTaskScheduler bottlingThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }
}
