package io.spring.cloud.samples.brewery.presenting;

import java.util.function.Supplier;

import brave.sampler.Sampler;
import io.spring.cloud.samples.brewery.common.TestConfiguration;
import io.spring.cloud.samples.brewery.common.events.Event;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableAsync
@EnableFeignClients
@Import(TestConfiguration.class)
public class Application {
	@Bean
	public Sampler sampler() {
		return Sampler.ALWAYS_SAMPLE;
	}

	@Bean
	@LoadBalanced
	public RestTemplate loadBalancedRestTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		new SpringApplication(Application.class).run(args);
	}

	@Bean
	EmitterProcessor<Event> emitterProcessor() {
		return EmitterProcessor.create();
	}

	@Bean
	Supplier<Flux<Event>> events(EmitterProcessor<Event> processor) {
		return () -> processor;
	}
}
