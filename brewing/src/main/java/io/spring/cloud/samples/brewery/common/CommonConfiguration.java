package io.spring.cloud.samples.brewery.common;

import io.micrometer.context.ContextSnapshot;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration(proxyBeanMethods = false)
class CommonConfiguration {

    @Bean
    @LoadBalanced
    RestTemplate loadBalancedRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Configuration
    @EnableAsync
    static class AsyncConfig implements AsyncConfigurer, WebMvcConfigurer {
        @Override
        public Executor getAsyncExecutor() {
            return ContextSnapshot.captureAll().wrapExecutorService(Executors.newCachedThreadPool());
        }

        @Override
        public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
            configurer.setTaskExecutor(new SimpleAsyncTaskExecutor(r -> new Thread(ContextSnapshot.captureAll().wrap(r))));
        }
    }


    /**
     * NAME OF THE BEAN IS IMPORTANT!
     * <p>
     * We need to wrap this for Async related things to propagate the context.
     *
     * @see EnableAsync
     */
    @Bean(name = "taskExecutor")
    Executor taskExecutor() {
        return ContextSnapshot.captureAll().wrapExecutorService(Executors.newCachedThreadPool());
    }
}
