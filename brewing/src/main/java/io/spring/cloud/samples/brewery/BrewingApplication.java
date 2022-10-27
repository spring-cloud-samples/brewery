package io.spring.cloud.samples.brewery;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.micrometer.context.ContextSnapshot;
import io.spring.cloud.samples.brewery.common.TestConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@Import(TestConfiguration.class)
public class BrewingApplication {

	public static void main(String[] args) {
		new SpringApplication(BrewingApplication.class).run(args);
	}


	/**
	 * NAME OF THE BEAN IS IMPORTANT!
	 *
	 * We need to wrap this for Async related things to propagate the context.
	 *
	 * @see EnableAsync
	 */
	@Bean
	Executor taskExecutor() {
		return ContextSnapshot.captureAll().wrapExecutorService(Executors.newCachedThreadPool());
	}
}
