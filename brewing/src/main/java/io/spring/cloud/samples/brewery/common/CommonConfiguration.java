package io.spring.cloud.samples.brewery.common;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.micrometer.context.ContextSnapshot;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
class CommonConfiguration {

	@Bean
	@LoadBalanced
	RestTemplate loadBalancedRestTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@Configuration
	@EnableAsync
	static class AsyncConfig implements AsyncConfigurer {
		@Override
		public Executor getAsyncExecutor() {
			return ContextSnapshot.captureAll().wrapExecutorService(Executors.newCachedThreadPool());
		}
	}


	/**
	 * NAME OF THE BEAN IS IMPORTANT!
	 *
	 * We need to wrap this for Async related things to propagate the context.
	 *
	 * @see EnableAsync
	 */
	@Bean(name = "taskExecutor")
	Executor taskExecutor() {
		return ContextSnapshot.captureAll().wrapExecutorService(Executors.newCachedThreadPool());
	}
}
