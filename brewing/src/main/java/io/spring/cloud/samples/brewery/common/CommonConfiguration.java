package io.spring.cloud.samples.brewery.common;

import io.micrometer.context.ContextSnapshot;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.*;

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
            return ContextSnapshot.wrapExecutorService(Executors.newCachedThreadPool(), () -> ContextSnapshot.captureAll());
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
    // [Observability] instrumenting executors
    @Bean(name = "taskExecutor", destroyMethod = "shutdown")
    ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler() {
            @Override
            protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
                ExecutorService executorService = super.initializeExecutor(threadFactory, rejectedExecutionHandler);
                return ContextSnapshot.wrapExecutorService(executorService, () -> ContextSnapshot.captureAll());
            }
        };
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }
}
